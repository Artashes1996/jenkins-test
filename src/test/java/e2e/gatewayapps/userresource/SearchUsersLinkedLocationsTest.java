package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.gatewayapps.userresource.data.UserDataProvider;
import helpers.appsapi.usersresource.UserHelper;

import helpers.appsapi.usersresource.payloads.SearchUsersLinkedLocationsBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;
import utils.Xray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static configuration.Role.OWNER;
import static configuration.Role.SUPPORT;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static helpers.appsapi.usersresource.payloads.SearchUsersLinkedLocationsBody.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class SearchUsersLinkedLocationsTest {

    private String orgId;
    private String ownerToken;
    private String unlinkedLocationId;
    private OrganizationFlows organizationFlows;
    private int usersActiveLocationsCount;
    private int usersInactiveLocationsCount;
    private int totalLocationCount;
    private JSONObject userWithActiveInactiveLocations;
    private static final Logger LOGGER = Logger.getLogger(SearchUsersLinkedLocationsTest.class);


    @BeforeClass
    public void setup() {

        organizationFlows = new OrganizationFlows();
        final JSONObject orgAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        ownerToken = orgAndUsers.getJSONObject(OWNER.name()).getString("token");

        orgId = orgAndUsers.getJSONObject("ORGANIZATION").getString("id");
        unlinkedLocationId = new LocationFlows().createLocation(orgId).getString("id");

        usersActiveLocationsCount = 4;
        usersInactiveLocationsCount = 3;

        LOGGER.info("the last + 1 is for the location which is a part of organization creation flow");
        totalLocationCount = usersActiveLocationsCount + usersInactiveLocationsCount + 1;
        userWithActiveInactiveLocations = new UserFlows().userWithActiveInactiveLocations(orgId, usersActiveLocationsCount, usersInactiveLocationsCount);

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3089")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void linkedLocationsSearch(Role role) {

        final JSONArray linkedLocationsIds = userWithActiveInactiveLocations.getJSONArray("LOCATIONS_IDS");
        final ArrayList<String> locationIds = new ArrayList<>();
        linkedLocationsIds.forEach(item -> locationIds.add((String) item));

        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : new AuthenticationFlowHelper().getUserTokenByRole(orgId, role, locationIds);
        final ArrayList<String> locations = UserHelper.searchUsersLinkedLocations(token, orgId, userWithActiveInactiveLocations.getString("USER_ID"),
                        new SearchUsersLinkedLocationsBody().bodyBuilder(UserLinkedLocationCombination.REQUIRED_FIELDS))
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.findAll {it.linked == true}.id");


        Collections.sort(locationIds);
        Collections.sort(locations);
        assertEquals(locations, locationIds);

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3090")
    @Test
    public void defaultSorting() {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);
        filterBody.getJSONObject(PAGINATION).put(PAGE, 0);
        filterBody.getJSONObject(PAGINATION).put(SIZE, totalLocationCount);

        final List<String> response = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.linked");

        final List<String> copy = new ArrayList<>(response);
        response.sort(Collections.reverseOrder());

        assertEquals(response, copy);

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3090")
    @Test
    public void withPagination() {

        final int pageSize = 3;
        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);
        filterBody.getJSONObject(PAGINATION).put(PAGE, 0);
        filterBody.getJSONObject(PAGINATION).put(SIZE, pageSize);

        final int count = totalLocationCount / pageSize + (totalLocationCount % pageSize == 0 ? 0 : 1);

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .body("totalPages", equalTo(count));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3091")
    @Test
    public void withInvalidPagination() {

        final int pageSize = 3;
        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);
        filterBody.getJSONObject(PAGINATION).put(PAGE, new JSONArray());
        filterBody.getJSONObject(PAGINATION).put(SIZE, pageSize);

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3107")
    @Test
    public void searchInDifferentOrganization() {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(UserLinkedLocationCombination.REQUIRED_FIELDS);
        final JSONObject otherOrgAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();

        UserHelper.searchUsersLinkedLocations(ownerToken, otherOrgAndOwner.getJSONObject("ORGANIZATION").getString("id"), otherOrgAndOwner.getJSONObject(OWNER.name()).getString("id"), filterBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-2512", test = "PEG-3092")
    @Test(dataProvider = "sorting", dataProviderClass = UserDataProvider.class)
    public void sortingByInternalName(String sortingMethod) {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);

        filterBody.getJSONObject(PAGINATION).put(SORT, "INTERNAL_NAME:" + sortingMethod);

        final List<String> response = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body("totalElements", is(totalLocationCount))
                .extract()
                .path("content.internalName");

        final List<String> copy = new ArrayList<>(response);
        Collections.sort(response);
        if (sortingMethod.equals("DESC")) {
            Collections.reverse(response);
        }
        assertEquals(response, copy);

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3094")
    @Test(dataProvider = "sorting", dataProviderClass = UserDataProvider.class)
    public void sortingByLinked(String sortingMethod) {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);
        filterBody.getJSONObject(PAGINATION).put(SORT, "LINKED:" + sortingMethod);

        final List<String> response = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body("totalElements", is(totalLocationCount))
                .extract()
                .path("content.linked");

        final List<String> copy = new ArrayList<>(response);
        Collections.sort(response);
        if (sortingMethod.equals("DESC")) {
            Collections.reverse(response);
        }
        assertEquals(response, copy);

    }

    @Xray(requirement = "PEG-2512", test = "PEG-?")
    @Test(dataProvider = "sorting", dataProviderClass = UserDataProvider.class)
    public void sortingById(String sortingMethod) {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);
        filterBody.getJSONObject(PAGINATION).put(SORT, "ID:" + sortingMethod);

        final List<String> response = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body("totalElements", is(totalLocationCount))
                .extract()
                .path("content.id");

        final List<String> copy = new ArrayList<>(response);
        Collections.sort(response);
        if (sortingMethod.equals("DESC")) {
            Collections.reverse(response);
        }
        assertEquals(response, copy);

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3661")
    @Test(dataProvider = "sorting", dataProviderClass = UserDataProvider.class)
    public void sortingByCreationDate(String sortingMethod) {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.WITH_PAGINATION);
        filterBody.getJSONObject(PAGINATION).put(SORT, "CREATION_DATE:" + sortingMethod);

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-2512", test = "PEG-4568")
    @Test
    public void nonExistingStatusFilter() {
        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);
        filterBody.put(SearchUsersLinkedLocationsBody.STATUSES, new JSONArray(List.of("INVALID")));

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(requirement = "PEG-2512", test = "PEG-3100")
    @Test
    public void filterByActiveState() {

        final JSONArray linkedLocationsIds = userWithActiveInactiveLocations.getJSONArray("LOCATIONS_IDS");
        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);
        filterBody.put(SearchUsersLinkedLocationsBody.STATUSES, new JSONArray(List.of("ACTIVE")));

        final ArrayList<Object> locations = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.findAll {it.linked == true}.id");

        Assert.assertEquals(locations.size(), usersActiveLocationsCount);

        final ArrayList<Object> locationIds = new ArrayList<>();
        linkedLocationsIds.forEach(locationIds::add);

        locations.forEach(item -> Assert.assertTrue(locationIds.contains(item)));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-3101")
    @Test
    public void filterByInactiveState() {

        final JSONArray linkedLocationsIds = userWithActiveInactiveLocations.getJSONArray("LOCATIONS_IDS");
        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);
        filterBody.put(SearchUsersLinkedLocationsBody.STATUSES, new JSONArray(List.of("INACTIVE")));

        final ArrayList<Object> locations = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.findAll {it.linked == true}.id");

        Assert.assertEquals(locations.size(), usersInactiveLocationsCount);

        final ArrayList<Object> locationIds = new ArrayList<>();
        linkedLocationsIds.forEach(locationIds::add);

        locations.forEach(item -> Assert.assertTrue(locationIds.contains(item)));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-4567")
    @Test
    public void filterByUnlinkedLocationId() {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);

        filterBody.put(LOCATION_IDS, new JSONArray(List.of(unlinkedLocationId)));

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", equalTo(1))
                .body("content[0].linked", equalTo(false))
                .body("content[0].id", equalTo(unlinkedLocationId));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-4570")
    @Test
    public void filterByLinkedLocationIds() {

        final JSONArray linkedLocationsIds = userWithActiveInactiveLocations.getJSONArray("LOCATIONS_IDS");
        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);
        filterBody.put(LOCATION_IDS, linkedLocationsIds);

        final ArrayList<Object> locations = UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.id");

        Assert.assertEquals(locations.size(), usersActiveLocationsCount + usersInactiveLocationsCount);

        final ArrayList<Object> locationIds = new ArrayList<>();
        linkedLocationsIds.forEach(locationIds::add);

        locations.forEach(item -> Assert.assertTrue(locationIds.contains(item)));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-4567")
    @Test
    public void filterByOtherOrganizationLocation() {

        final JSONObject orgAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String otherOrgId = orgAndOwner.getJSONObject("ORGANIZATION").getString("id");

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);
        final String locationId = new LocationFlows().createLocation(otherOrgId).getString("id");
        filterBody.put(LOCATION_IDS, new JSONArray(List.of(locationId)));

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", equalTo(0));

    }

    @Xray(requirement = "PEG-2512", test = "PEG-4572")
    @Test
    public void filterByInvalidLocationIds() {

        final JSONObject filterBody = new SearchUsersLinkedLocationsBody().bodyBuilder(SearchUsersLinkedLocationsBody.UserLinkedLocationCombination.REQUIRED_FIELDS);
        filterBody.put(LOCATION_IDS, true);

        UserHelper.searchUsersLinkedLocations(SUPPORT_TOKEN, orgId, userWithActiveInactiveLocations.getString("USER_ID"), filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

}
