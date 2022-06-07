package e2e.gatewayapps.locationservicegroupresource;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationservicegroupsresource.LocationServiceGroupsHelper;
import helpers.flows.LocationFlows;
import helpers.flows.LocationServiceGroupFlows;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.UUID;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.hc.core5.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetLocationServiceGroupTest {

    private String organizationId;
    private String locationId;
    private String locationServiceGroupId;
    private JSONObject organizationAndUsersObject;
    private OrganizationFlows organizationFlows;
    private LocationServiceGroupFlows locationServiceGroupFlows;
    private LocationFlows locationFlows;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationServiceGroupFlows = new LocationServiceGroupFlows();
        locationFlows = new LocationFlows();
        organizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId,
                null).getString("id");
    }

    @Xray(test = "PEG-5922", requirement = "PEG-5750")
    @Test
    public void getLocationServiceGroupByNonExistingOrganizationBySupportRole() {
        final String fakeOrganizationId = UUID.randomUUID().toString();
        LocationServiceGroupsHelper.getGroupById(SUPPORT_TOKEN, fakeOrganizationId,
                        locationId, locationServiceGroupId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5923", requirement = "PEG-5750")
    @Test
    public void getLocationServiceGroupByNonExistingOrganizationByUnSupportRoles() {
        final Role role = getRandomOrganizationRole();
        final String userToken = organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String fakeOrganizationId = UUID.randomUUID().toString();
        LocationServiceGroupsHelper.getGroupById(userToken, fakeOrganizationId,
                        locationId, locationServiceGroupId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-5924", requirement = "PEG-5750")
    @Test
    public void getLocationServiceGroupByNonExistingLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String fakeLocationId = UUID.randomUUID().toString();
        LocationServiceGroupsHelper.getGroupById(userToken, organizationId,
                        fakeLocationId, locationServiceGroupId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5926", requirement = "PEG-5750")
    @Test
    public void getLocationServiceGroupByNonExistingGroup() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String fakeGroupId = UUID.randomUUID().toString();
        LocationServiceGroupsHelper.getGroupById(userToken, organizationId,
                        locationId, fakeGroupId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5928", requirement = "PEG-5750")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getLocationServiceGroupFirstLevel(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        LocationServiceGroupsHelper.getGroupById(userToken, organizationId,
                        locationId, locationServiceGroupId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getLocationServiceGroup.json"));
    }

    @Xray(test = "PEG-5994", requirement = "PEG-5750")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getLocationServiceGroupSecondLevel(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String secondLevelGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, locationServiceGroupId).getString("id");
        LocationServiceGroupsHelper.getGroupById(userToken, organizationId,
                        locationId, secondLevelGroupId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getLocationServiceGroup.json"))
                .body("parentId", is(locationServiceGroupId));
    }

    @Xray(test = "PEG-5929", requirement = "PEG-5750")
    @Test
    public void getLocationServiceGroupDeletedOrganizationBySupport() {
        final JSONObject deletedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithOwner();
        final String deletedOrganizationId = deletedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");
        final String deletedOrganizationLocationServiceGroupId = locationServiceGroupFlows.createGroup(deletedOrganizationId, deletedOrganizationLocationId,
                null).getString("id");
        organizationFlows.deleteOrganization(deletedOrganizationId);
        LocationServiceGroupsHelper.getGroupById(SUPPORT_TOKEN, deletedOrganizationId,
                        deletedOrganizationLocationId, deletedOrganizationLocationServiceGroupId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getLocationServiceGroup.json"));
    }

    @Xray(test = "PEG-5995", requirement = "PEG-5750")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getUpdatedLocationServiceGroup(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId,
                locationId, locationServiceGroupId).getString("id");
        final JSONObject updatedServiceGroupObject = locationServiceGroupFlows.updateGroup(organizationId, locationId, newLocationServiceGroupId);
        LocationServiceGroupsHelper.getGroupById(userToken, organizationId,
                        locationId, newLocationServiceGroupId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getLocationServiceGroup.json"))
                .body("id", is(updatedServiceGroupObject.getString("id")))
                .body("parentId", is(updatedServiceGroupObject.getString("parentId")))
                .body("locationId", is(updatedServiceGroupObject.getString("locationId")))
                .body("name", is(updatedServiceGroupObject.getString("name")))
                .body("displayName", is(updatedServiceGroupObject.getString("displayName")));
    }
}

