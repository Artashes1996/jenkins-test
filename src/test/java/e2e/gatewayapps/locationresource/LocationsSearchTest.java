package e2e.gatewayapps.locationresource;


import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.locationresource.data.LocationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.locationsresource.payloads.LocationsSearchRequestBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;

import static configuration.Role.*;
import static helpers.appsapi.locationsresource.payloads.LocationsSearchRequestBody.SortingBy.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;

public class LocationsSearchTest extends BaseTest {

    private Map<String, JSONObject> locations;
    private String organizationId;
    private List<String> locationsIds;
    private AuthenticationFlowHelper authenticationFlowHelper;
    private JSONObject organizationAndUsers;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        final int locationsCount = 8;
        locations = new HashMap<>();
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        authenticationFlowHelper = new AuthenticationFlowHelper();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationsIds = new ArrayList<>();
        locationsIds.add(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        for (int i = 0; i < locationsCount; i++) {
            final JSONObject physicalLocation = locationFlows.createLocation(organizationId);
            final String physicalLocationId = physicalLocation.getString("id");

            locationsIds.add(physicalLocationId);
            locations.put(physicalLocationId, physicalLocation);

            final JSONObject virtualLocation = locationFlows.createLocation(organizationId);
            final String virtualLocationId = virtualLocation.getString("id");
            locationsIds.add(virtualLocationId);
            locations.put(virtualLocationId, virtualLocation);
        }
        Collections.sort(locationsIds);
    }

    //    TODO all xrays should be rewrite
    @Xray(test = "PEG-1321")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsList(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, 50);
        searchBody.put(LocationsSearchRequestBody.SEARCH_MODE, LocationsSearchRequestBody.LocationSearchModes.NO_FILTERING);
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SORT, ID.getAscending());

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}", equalTo(locationsIds));
    }

    @Xray(test = "PEG-1739")
    @Test(dataProvider = "locationSearchModes", dataProviderClass = LocationDataProvider.class)
    public void seeDeletedOrganizationLocationsList(LocationsSearchRequestBody.LocationSearchModes searchModes) {
        final String deletedOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        int numberOfVirtualPhysicalLocations = 5;
        final JSONArray locations = locationFlows.createLocations(deletedOrganizationId, numberOfVirtualPhysicalLocations);
        final ArrayList<String> createdLocations = new ArrayList<>();
        locations.forEach(location -> createdLocations.add(((JSONObject) location).getString("id")));

        Collections.sort(createdLocations);

        organizationFlows.deleteOrganization(deletedOrganizationId);
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, 50);
        searchBody.put(LocationsSearchRequestBody.SEARCH_MODE, searchModes);
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SORT, ID.getAscending());

        LocationsHelper.searchLocation(SUPPORT_TOKEN, deletedOrganizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}", equalTo(createdLocations));
    }

    @Xray(test = "PEG-3020")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void searchWithSeeLocationsModeByStaffAndLocationAdmin(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.put(LocationsSearchRequestBody.SEARCH_MODE, LocationsSearchRequestBody.LocationSearchModes.FILTERED_BY_SEARCH_LOCATIONS_PERMISSION);

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}.size", equalTo(1))
                .body("content.collect{it.id}[0]", equalTo(organizationAndUsers.getJSONObject("LOCATION").getString("id")));
    }

    @Xray(test = "PEG-3021")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void searchPermissionWithWorkingId(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject searchBody = new JSONObject();
        searchBody.put(LocationsSearchRequestBody.SEARCH_MODE, LocationsSearchRequestBody.LocationSearchModes.FILTERED_BY_SEARCH_LOCATIONS_PERMISSION);
        searchBody.put(LocationsSearchRequestBody.WORKING_USER_ID, organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    //TODO enable after the feature of the permission will be enabled
    @Test(enabled = false, dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void searchSelfLocationsWithServiceLinkingPermissionMode(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final List<String> locationAdmin1Locations = Arrays.asList(locationsIds.get(1), locationsIds.get(2), locationsIds.get(3), locationsIds.get(4));
        final JSONObject locationAdmin1 = userFlows.createUser(organizationId, LOCATION_ADMIN, locationAdmin1Locations);
        final List<String> locationAdmin2Locations = Arrays.asList(locationsIds.get(3), locationsIds.get(4), locationsIds.get(5), locationsIds.get(6));
        final JSONObject locationAdmin2 = userFlows.createUser(organizationId, LOCATION_ADMIN, locationAdmin2Locations);

        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.put(LocationsSearchRequestBody.SEARCH_MODE, LocationsSearchRequestBody.LocationSearchModes.FILTERED_BY_LOCATION_USER_SERVICE_LINK_PERMISSION);
//        Checking staff
        searchBody.put(LocationsSearchRequestBody.WORKING_USER_ID, organizationAndUsers.getJSONObject(STAFF.name()).getString("id"));

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it}.size", equalTo(0));
//          Check 0th location admin with one linked location
        searchBody.put(LocationsSearchRequestBody.WORKING_USER_ID, organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("collect{content.id}.size", equalTo(1))
                .body("collect{content.id}[0]", equalTo(organizationAndUsers.getJSONObject("LOCATION").getString("id")));

//        Check location admin linked locations intersection
        final String locationAdmin1Token = authenticationFlowHelper.getTokenWithEmail(locationAdmin1.getString("email"));
        final List<String> twoLocAdminsLocationsIntersection = locationAdmin1Locations.stream().distinct().filter(locationAdmin2Locations::contains).collect(Collectors.toList());
        final String locationAdmin2userId = locationAdmin2.getString("id");
        searchBody.put(LocationsSearchRequestBody.WORKING_USER_ID, locationAdmin2userId);

        LocationsHelper.searchLocation(token, locationAdmin1Token, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("collect{content.id}.size", equalTo(1))
                .body("collect{content.id}", equalTo(twoLocAdminsLocationsIntersection));
    }

    //    TODO enable when the permission will be implemented
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class, enabled = false)
    public void searchWithServiceLinkingPermissionModeDeletedUserWorkingId(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.put(LocationsSearchRequestBody.SEARCH_MODE, LocationsSearchRequestBody.LocationSearchModes.FILTERED_BY_LOCATION_USER_SERVICE_LINK_PERMISSION);
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final String deletedLocationAdminId = userFlows.createUser(organizationId, LOCATION_ADMIN, locationIds).getString("id");
        userFlows.deleteUser(organizationId, deletedLocationAdminId);

        searchBody.put(LocationsSearchRequestBody.WORKING_USER_ID, deletedLocationAdminId);
        LocationsHelper.searchLocation(token, token, searchBody)
                .then()
                .statusCode(SC_NOT_FOUND);

    }

    @Xray(test = "PEG-1323")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsSizeExceedingAmount(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, locations.size() + 10);
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SORT, ID.getAscending());

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}", equalTo(locationsIds));
    }

    @Xray(test = "PEG-1324")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsPageExceedingContent(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, locations.size());
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.PAGE, 3);

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.size", equalTo(0));


    }

    @Xray(test = "PEG-1326, PEG-1327")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsRequestDefaultValues(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = new JSONObject();

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}.size", equalTo(locationsIds.size()));
    }

    @Xray(test = "PEG-1328")
    @Test
    public void seeLocationsBySupportNonExistingOrganizationId() {
        final JSONObject searchBody = new JSONObject();
        final String organizationId = UUID.randomUUID().toString();

        LocationsHelper.searchLocation(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-1329")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsByOwnerNonExistingOrganizationId(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        final String organizationId = UUID.randomUUID().toString();

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-1331")
    @Test
    public void seeLocationsByOwnerOtherOrganizationId() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();

        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, 50);

        final String ownerToken = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));

        LocationsHelper.searchLocation(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-1342")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsSortedByCreationDate(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SORT, CREATION_DATE.getAscending());

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-1343")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchLocationByName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final String queryLocationId = locationsIds.get(getRandomInt(0, locations.size() - 1));
        final String queryName = locations.get(queryLocationId).getString("internalName");

        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        searchBody.put(LocationsSearchRequestBody.QUERY, queryName);

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content[0].internalName", equalTo(queryName))
                .body("content.size", equalTo(1));

    }

    @Xray(test = "PEG-1344")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchLocationsByPartialName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();
        final String partialName = "name";
        searchBody.put(LocationsSearchRequestBody.QUERY, partialName);
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, 50);
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SORT, ID.getAscending());
        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}", equalTo(locationsIds));
    }

    @Xray(test = "PEG-1345")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchLocationById(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = LocationsSearchRequestBody.bodyBuilder();

        final String locationId = locationsIds.get(getRandomInt(locationsIds.size()));
        searchBody.getJSONObject(LocationsSearchRequestBody.PAGINATION).put(LocationsSearchRequestBody.SIZE, 50);
        searchBody.put(LocationsSearchRequestBody.QUERY, locationId);

        LocationsHelper.searchLocation(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocations.json"))
                .body("content.collect{it.id}.size", equalTo(1))
                .body("content.collect{it.id}[0]", equalTo(locationId));
    }
}