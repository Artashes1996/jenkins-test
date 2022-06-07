package e2e.gatewayapps.locationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.locationsresource.payloads.SearchUserLinkedLocationsBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;
import utils.commons.ToggleAction;

import java.util.*;
import java.util.stream.IntStream;

import static configuration.Role.*;
import static helpers.appsapi.locationsresource.payloads.LocationUpdateRequestBody.*;
import static helpers.appsapi.locationsresource.payloads.SearchUserLinkedLocationsBody.LOCATION_IDS;
import static helpers.appsapi.locationsresource.payloads.SearchUserLinkedLocationsBody.QUERY;
import static helpers.appsapi.locationsresource.payloads.SearchUserLinkedLocationsBody.SearchUserLinkedLocationsCombination.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class SearchUserLinkedBasicLocations extends BaseTest {

    private SearchUserLinkedLocationsBody searchUserLinkedLocationsBody;
    private String organizationId;
    private String locationId;
    private String userIdForNegativeCases;
    private String userId;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private List<String> locationIds;
    private List<String> linkedLocationsToUserIds;
    private List<String> linkedLocationsToUserInternalNames;
    private List<String> locationInternalNames;
    private JSONObject organizationAndUsers;
    private JSONArray locations;


    @BeforeClass
    public void setUp() {
        locationIds = new ArrayList<>();
        linkedLocationsToUserIds = new ArrayList<>();
        linkedLocationsToUserInternalNames = new ArrayList<>();
        locationInternalNames = new ArrayList<>();
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        searchUserLinkedLocationsBody = new SearchUserLinkedLocationsBody();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject location = organizationAndUsers.getJSONObject("LOCATION");
        locationId = location.getString("id");
        final String internalName = location.getString("internalName");
        locations = locationFlows.createLocations(organizationId, 4);
        IntStream.range(0, locations.length()).forEach(i -> {
            locationIds.add(locations.getJSONObject(i).getString("id"));
            locationInternalNames.add(locations.getJSONObject(i).getString("internalName"));
        });
        userIdForNegativeCases = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("id");
        linkedLocationsToUserIds = Arrays.asList(locationIds.get(0), locationIds.get(2));
        linkedLocationsToUserInternalNames = Arrays.asList(locationInternalNames.get(0), locationInternalNames.get(2));
        userId = userFlows.createUser(organizationId, getRandomOrganizationRole(), linkedLocationsToUserIds).getString("id");
        userFlows.linkUnlinkLocationsToUser(organizationId, userIdForNegativeCases, locationIds, ToggleAction.LINK);
        locationIds.add(locationId);
        locationInternalNames.add(internalName);

        Collections.sort(locationInternalNames);
        Collections.sort(locationIds);
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6092")
    @Test
    public void searchUserLinkedLocationsWithNonExistingOrgIdBySupport() {
        final String fakeOrganizationId = UUID.randomUUID().toString();
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(SUPPORT_TOKEN, fakeOrganizationId, userIdForNegativeCases, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6106")
    @Test
    public void searchUserLinkedLocationsWithNonExistingOrgIdByUnsupportedRoles() {
        final Role role = getRandomOrganizationRole();
        final String userToken = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String fakeOrganizationId = UUID.randomUUID().toString();
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(userToken, fakeOrganizationId, userIdForNegativeCases, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6107")
    @Test
    public void searchUserLinkedLocationsWithNonExistingUserId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String fakeUserId = UUID.randomUUID().toString();
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, fakeUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6109")
    @Test
    public void searchUserLinkedLocationsWithInvalidLocationId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String invalidLocationId = "invalid_location_id";
        final JSONObject searchBody = new JSONObject();
        final JSONArray locationsIds = new JSONArray();
        searchBody.put(LOCATION_IDS, locationsIds);
        searchBody.getJSONArray(LOCATION_IDS).put(invalidLocationId);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userIdForNegativeCases, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6110")
    @Test
    public void searchUserLinkedLocationsWithEmptyLocationId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_LOCATIONS);
        searchBody.getJSONArray(LOCATION_IDS).put("");
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userIdForNegativeCases, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6111")
    @Test
    public void searchUserLinkedLocationsWithInvalidQuery() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        searchBody.put(QUERY, "invalid_query_value");
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userIdForNegativeCases, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6112")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchUserLinkedLocations(Role role) {
        final Role randomRole = getRandomOrganizationRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchUserLinkedBasicLocations.json"))
                .body("id", containsInAnyOrder(linkedLocationsToUserIds.get(0), linkedLocationsToUserIds.get(1)))
                .body("internalName", containsInAnyOrder(linkedLocationsToUserInternalNames.get(0), linkedLocationsToUserInternalNames.get(1)));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6113")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchUserLinkedLocationsByLocations(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_LOCATIONS);
        linkedLocationsToUserIds.forEach(locationId -> searchBody.getJSONArray(LOCATION_IDS).put(locationId));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchUserLinkedBasicLocations.json"))
                .body("size()", is(2))
                .body("id", containsInAnyOrder(linkedLocationsToUserIds.get(0), linkedLocationsToUserIds.get(1)));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6114")
    @Test
    public void searchUserLinkedLocationsUsingLocationIdAsQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationId);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("id[0]", is(searchingLocationId));
        searchBody.put(QUERY, searchingLocationId.substring(2, 9));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("id[0]", is(searchingLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6142")
    @Test
    public void searchUserLinkedLocationsUsingInternalNameAsQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationInternalName = locations.getJSONObject(0).getString("internalName");
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationInternalName);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("id[0]", is(searchingLocationId))
                .body("internalName[0]", is(searchingLocationInternalName));
        searchBody.put(QUERY, searchingLocationInternalName.substring(10));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("id[0]", is(searchingLocationId))
                .body("internalName[0]", is(searchingLocationInternalName));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6143")
    @Test
    public void searchUserLinkedLocationsUsingCountryAsQuery() {
        final Role randomRole = getRandomOrganizationRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationCountryName = linkedLocationsToUserInternalNames.get(0);
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationCountryName);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
        searchBody.put(QUERY, searchingLocationCountryName.substring(1, 8));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6164")
    @Test
    public void searchUserLinkedLocationsUsingCityAsQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationCityName = locations.getJSONObject(0).getJSONObject("address").getString("city");
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationCityName);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
        searchBody.put(QUERY, searchingLocationCityName.substring(1, 4));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6165")
    @Test
    public void searchUserLinkedLocationsUsingZipCodeAsQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationZipCode = locations.getJSONObject(0).getJSONObject("address").getString("zipcode");
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationZipCode);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
        searchBody.put(QUERY, searchingLocationZipCode.substring(1, 4));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6166")
    @Test
    public void searchUserLinkedLocationsUsingStateRegionAsQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationRegion = locations.getJSONObject(0).getJSONObject("address").getString("stateRegion");
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationRegion);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
        searchBody.put(QUERY, searchingLocationRegion.substring(1, 4));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6167")
    @Test
    public void searchUserLinkedLocationsUsingAddressAsQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        final String searchingLocationAddress = locations.getJSONObject(0).getJSONObject("address").getString("address");
        final String searchingLocationId = linkedLocationsToUserIds.get(0);
        searchBody.put(QUERY, searchingLocationAddress);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
        searchBody.put(QUERY, searchingLocationAddress.substring(0, 3));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", contains(searchingLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6123")
    @Test
    public void searchUserLinkedLocationAfterUnlinkingUserToLocation() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final List<String> linkedLocationIds = Arrays.asList(locationIds.get(0), locationIds.get(1));
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), linkedLocationIds).getString("id");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_LOCATIONS);
        userFlows.linkUnlinkLocationToUser(organizationId, newUserId, locationIds.get(0), ToggleAction.UNLINK);
        searchBody.getJSONArray(LOCATION_IDS).put(locationIds.get(0)).put(locationIds.get(1));
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, newUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", hasItem(not(locationIds.get(0))));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6124")
    @Test
    public void searchUserLinkedLocationUpdatedLocationData() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String locationIdToUpdate = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationIdToUpdate)).getString("id");
        final JSONObject updatedLocation = locationFlows.updateLocation(organizationId, locationIdToUpdate);
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_QUERY);
        searchBody.put(QUERY, locationIdToUpdate);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, newUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(locationIdToUpdate))
                .body("nameTranslation[0]", is(updatedLocation.getString(NAME_TRANSLATION)))
                .body("description[0]", is(updatedLocation.getString(DESCRIPTION)))
                .body("phoneNumber[0]", is(updatedLocation.getString(PHONE_NUMBER)))
                .body("status[0]", is(updatedLocation.getString(STATUS)));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6125")
    @Test
    public void searchUserLinkedLocationDeletedOrganizationBySupport() {
        final JSONObject deletedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String deletedOrganizationId = deletedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = deletedOrganizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        final String deletedOrganizationUserId = deletedOrganizationAndUsersObject.getJSONObject("OWNER").getString("id");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        organizationFlows.deleteOrganization(deletedOrganizationId);
        LocationsHelper.searchUserLinkedLocation(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(deletedOrganizationLocationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6253")
    @Test
    public void searchUserLinkedLocationForInactiveUser() {
        final Role randomRole = getRandomRole();
        final String inactiveUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("id");
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        userFlows.inactivateUserById(organizationId, inactiveUserId);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, inactiveUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(locationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6258")
    @Test
    public void searchUserLinkedLocationForDeletedUser() {
        final Role randomRole = getRandomRole();
        final String deletedUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("id");
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        userFlows.deleteUser(organizationId, deletedUserId);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, deletedUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(locationId));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6259")
    @Test(dataProvider = "otherOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void searchUserLinkedLocationByOtherOrganizationUser(Role role) {
        final JSONObject otherOrganizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String userToken = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String ownerId = otherOrganizationAndOwner.getJSONObject(OWNER.name()).getString("id");
        final String otherOrganizationId = otherOrganizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(userToken, otherOrganizationId, ownerId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6263")
    @Test
    public void searchUserNotLinkedLocations() {
        final Role randomRole = getRandomRole();
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), null).getString("id");
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, newUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6272")
    @Test
    public void searchUserNotLinkedLocationsUsingLocationIds() {
        final Role randomRole = getRandomRole();
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), null).getString("id");
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        final JSONArray locationsIds = new JSONArray();
        searchBody.put(LOCATION_IDS, locationsIds);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, newUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6273")
    @Test
    public void searchUserLinkedLocationByLocationIdAndQuery() {
        final Role randomRole = getRandomRole();
        final String userToken = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(WITH_LOCATIONS);
        final String firstLocationId = locations.getJSONObject(0).getString("id");
        final String firstLocationInternalName = locations.getJSONObject(0).getString("internalName");
        searchBody.getJSONArray(LOCATION_IDS).put(firstLocationId);
        searchBody.put(QUERY, firstLocationInternalName);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, userId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(firstLocationId))
                .body("internalName[0]", is(firstLocationInternalName));
    }

    @Xray(requirement = "PEG-5751", test = "PEG-6309")
    @Test
    public void searchOwnUserLinkedLocations() {
        final Role randomRole = getRandomRolesWithLocation();
        final String locationId = locationIds.get(0);
        final String newUserId = userFlows.createUser(organizationId, randomRole, locationIds)
                .getString("id");
        final String userToken = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject searchBody = searchUserLinkedLocationsBody.bodyBuilder(EMPTY_BODY);
        LocationsHelper.searchUserLinkedLocation(userToken, organizationId, newUserId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id", hasItem(locationId));
    }

}

