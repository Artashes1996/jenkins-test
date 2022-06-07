package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.gatewayapps.userresource.data.UserDataProvider;
import helpers.appsapi.usersresource.UserHelper;
import helpers.appsapi.usersresource.payloads.UserLinkedLocationServicesSearch;

import helpers.flows.*;
import io.restassured.response.*;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;
import utils.commons.ToggleAction;

import java.util.*;

import static helpers.appsapi.usersresource.payloads.SearchUsersLinkedLocationsBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class UserLinkedUnlinkedLocationServicesSearchTest extends BaseTest {

    private JSONObject orgAndUsers;
    private String orgId;
    private List<String> servicesIds;
    private String firstLocationId;
    private List<String> firstLocationServiceIds;
    private List<String> usersLinkedFirstLocationServiceIds;
    private List<String> usersNotLinkedFirstLocationServiceIds;
    private String secondLocationId;
    private String secondLocationServiceIdNotLinkedToTheUser;
    private String independentServiceId;

    @BeforeClass
    public void dataPreparation() {
        final LocationFlows locationFlows = new LocationFlows();
        final UserFlows userFlows = new UserFlows();
        orgAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        orgId = orgAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONArray locations = locationFlows.createLocations(orgId, 2);
        servicesIds = new ServiceFlows().createServices(orgId, 7);

        firstLocationId = locations.getJSONObject(0).getString("id");
        firstLocationServiceIds = servicesIds.subList(0, 4);
        usersLinkedFirstLocationServiceIds = firstLocationServiceIds.subList(0, 2);
        usersNotLinkedFirstLocationServiceIds = firstLocationServiceIds.subList(2, 4);
        locationFlows.linkUnlinkServicesToLocation(orgId, firstLocationId, firstLocationServiceIds, ToggleAction.LINK);

        userFlows.linkUnlinkLocationToUser(orgId, orgAndUsers.getJSONObject("OWNER").getString("id"), firstLocationId, ToggleAction.LINK);
        userFlows.linkUnlinkLocationToUser(orgId, orgAndUsers.getJSONObject("ADMIN").getString("id"), firstLocationId, ToggleAction.LINK);
        userFlows.linkUnlinkLocationToUser(orgId, orgAndUsers.getJSONObject("LOCATION_ADMIN").getString("id"), firstLocationId, ToggleAction.LINK);
        userFlows.linkUnlinkLocationToUser(orgId, orgAndUsers.getJSONObject("STAFF").getString("id"), firstLocationId, ToggleAction.LINK);

        orgAndUsers.getJSONObject("LOCATION_ADMIN").put("token", new AuthenticationFlowHelper().getTokenWithEmail(orgAndUsers.getJSONObject("LOCATION_ADMIN").getString("email")));
        orgAndUsers.getJSONObject("STAFF").put("token", new AuthenticationFlowHelper().getTokenWithEmail(orgAndUsers.getJSONObject("STAFF").getString("email")));

        userFlows.linkUnlinkUserToLocationServices(orgId, orgAndUsers.getJSONObject("OWNER").getString("id"), firstLocationId, usersLinkedFirstLocationServiceIds, ToggleAction.LINK);
        userFlows.linkUnlinkUserToLocationServices(orgId, orgAndUsers.getJSONObject("ADMIN").getString("id"), firstLocationId, usersLinkedFirstLocationServiceIds, ToggleAction.LINK);
        userFlows.linkUnlinkUserToLocationServices(orgId, orgAndUsers.getJSONObject("LOCATION_ADMIN").getString("id"), firstLocationId, usersLinkedFirstLocationServiceIds, ToggleAction.LINK);
        userFlows.linkUnlinkUserToLocationServices(orgId, orgAndUsers.getJSONObject("STAFF").getString("id"), firstLocationId, usersLinkedFirstLocationServiceIds, ToggleAction.LINK);

        secondLocationId = locations.getJSONObject(1).getString("id");
        secondLocationServiceIdNotLinkedToTheUser = servicesIds.get(5);
        locationFlows.linkUnlinkServicesToLocation(orgId, secondLocationId, List.of(secondLocationServiceIdNotLinkedToTheUser), ToggleAction.LINK);

        independentServiceId = servicesIds.get(6);
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4923")
    @Test(dataProvider = "some valid roles combinations", dataProviderClass = RoleDataProvider.class)
    public void checkLinkedUserLocationServices(Role requesterRole, Role userRole) {

        final String requesterToken = (requesterRole == Role.SUPPORT) ? SUPPORT_TOKEN : orgAndUsers.getJSONObject(requesterRole.name()).getString("token");
        final String userId = orgAndUsers.getJSONObject(userRole.name()).getString("id");
        final ExtractableResponse<Response> response = UserHelper.searchUserLinkedLocationServices(requesterToken, orgId, userId, firstLocationId
                        , new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.REQUIRED_FIELDS))
                .then()
                .statusCode(SC_OK)
                .extract();
        final ArrayList<String> linkedServicesIds = response.path("content.findAll {it.linked == true}.id");
        Collections.sort(linkedServicesIds);
        Collections.sort(usersLinkedFirstLocationServiceIds);
        assertEquals(linkedServicesIds, usersLinkedFirstLocationServiceIds);

        final ArrayList<String> notLinkedServicesIds = response.path("content.findAll {it.linked == false}.id");
        Collections.sort(notLinkedServicesIds);
        Collections.sort(usersNotLinkedFirstLocationServiceIds);
        assertEquals(notLinkedServicesIds, usersNotLinkedFirstLocationServiceIds);

    }

    @Xray(requirement = "PEG-2509", test = "PEG-5931")
    @Test
    public void defaultSorting() {

        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);
        body.getJSONObject(PAGINATION).put(SIZE, servicesIds.size());

        final ArrayList<String> response = UserHelper.searchUserLinkedLocationServices(user.getString("token"), orgId, user.getString("id"), firstLocationId, body)

                .then()
                .statusCode(SC_OK)
                .extract()
                .path("content.linked");

        final List<String> copy = new ArrayList<>(response);
        response.sort(Collections.reverseOrder());

        assertEquals(response, copy);

    }

    @Xray(requirement = "PEG-2509", test = "PEG-5932")
    @Test
    public void withPagination() {

        final int pageSize = 3;
        final int totalServicesCount = firstLocationServiceIds.size();
        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);
        body.getJSONObject(PAGINATION).put(PAGE, 0);
        body.getJSONObject(PAGINATION).put(SIZE, pageSize);

        final int count = totalServicesCount / pageSize + (totalServicesCount % pageSize == 0 ? 0 : 1);

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, user.getString("id"), firstLocationId, body)
                .then()
                .statusCode(SC_OK)
                .body("totalPages", equalTo(count));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4928")
    @Test(dataProvider = "invalidPage", dataProviderClass = UserDataProvider.class)
    public void withInvalidPagination(Object page) {

        final int pageSize = 3;
        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);
        body.getJSONObject(PAGINATION).put(PAGE, page);
        body.getJSONObject(PAGINATION).put(SIZE, pageSize);

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, user.getString("id"), firstLocationId, body)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4928")
    @Test
    public void withInvalidPaginationCount() {

        final int pageSize = 3;
        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);
        body.getJSONObject(PAGINATION).put(PAGE, -1);
        body.getJSONObject(PAGINATION).put(SIZE, pageSize);

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, user.getString("id"), firstLocationId, body)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4916")
    @Test
    public void searchInFakeOrganizationBySupport() {

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, UUID.randomUUID(), orgAndUsers.getJSONObject(Role.ADMIN.name()).getString("id"), firstLocationId, new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.REQUIRED_FIELDS))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4917")
    @Test
    public void searchInFakeOrganizationByOrganizationRoles() {

        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());

        UserHelper.searchUserLinkedLocationServices(user.getString("token"), UUID.randomUUID(), orgAndUsers.getJSONObject(Role.ADMIN.name()).getString("id"), firstLocationId, new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.REQUIRED_FIELDS))
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4918")
    @Test
    public void searchInFakeLocationBySupport() {

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, orgAndUsers.getJSONObject(Role.ADMIN.name()).getString("id"), UUID.randomUUID(), new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.REQUIRED_FIELDS))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-4919")
    @Test
    public void searchInFakeLocationByOrganizationRoles() {

        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());

        UserHelper.searchUserLinkedLocationServices(user.getString("token"), orgId, orgAndUsers.getJSONObject(Role.ADMIN.name()).getString("id"), UUID.randomUUID(), new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.REQUIRED_FIELDS))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-5933")
    @Test
    public void searchServiceNotLinkedToAnyLocation() {

        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);
        body.put(UserLinkedLocationServicesSearch.QUERY, independentServiceId);

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, user.getString("id"), firstLocationId, body)
                .then()
                .statusCode(SC_OK)
                .body("totalElements", is(0));

    }

    @Xray(requirement = "PEG-2509", test = "PEG-5934")
    @Test
    public void searchLinkedLocationServiceNotLinkedToTheUser() {

        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);
        body.put(UserLinkedLocationServicesSearch.QUERY, secondLocationServiceIdNotLinkedToTheUser);

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, user.getString("id"), secondLocationId, body)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-2509", test = "PEG-5935")
    @Test
    public void searchServiceNotLinkedToUserLocation() {

        final JSONObject user = orgAndUsers.getJSONObject(Role.getRandomOrganizationRole().name());
        final JSONObject body = new UserLinkedLocationServicesSearch().bodyBuilder(UserLinkedLocationServicesSearch.UserLinkedLocationCombination.WITH_PAGINATION);

        UserHelper.searchUserLinkedLocationServices(SUPPORT_TOKEN, orgId, user.getString("id"), secondLocationId, body)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }
}
