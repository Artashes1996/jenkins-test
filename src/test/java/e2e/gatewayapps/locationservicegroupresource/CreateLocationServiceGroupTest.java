package e2e.gatewayapps.locationservicegroupresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationservicegroupsresource.LocationServiceGroupsHelper;
import helpers.appsapi.locationservicegroupsresource.payloads.GroupCreationBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static org.apache.http.HttpStatus.*;
import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

public class CreateLocationServiceGroupTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;
    private LocationFlows locationFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private List<String> locations;
    private String supportToken;
    private AuthenticationFlowHelper authenticationFlowHelper;


    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        authenticationFlowHelper = new AuthenticationFlowHelper();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        supportToken = getToken(SUPPORT);
        locations = new ArrayList<>();
        locations.add(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        locations.add(locationFlows.createInactiveLocation(organizationId).getString("id"));
        locations.add(locationFlows.createLocation(organizationId).getString("id"));
    }

    @Xray(test = "PEG-3334, PEG-3337", requirement = "PEG-2827")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void createGroupBySupportedRoles(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        final String locationId = locations.get(0);

        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));
        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));
    }

    @Xray(test = "PEG-3335", requirement = "PEG-2827")
    @Test
    public void createGroupByUnsupported() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        final String locationId = locations.get(0);

        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3336", requirement = "PEG-2827")
    @Test
    public void createGroupWithLocationAdminOfOtherLocation() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        final String locationId = locations.get(1);

        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3338", requirement = "PEG-2827")
    @Test(dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void createGroupOnOtherOrganizationLocation(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject otherOrganizationLocation = locationFlows.createLocation(otherOrganizationId);
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(token, organizationId, otherOrganizationLocation.getString("id"), groupCreationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-3338", requirement = "PEG-2827")
    @Test
    public void createGroupOnOtherOrganizationLocationByLocationAdmin() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject otherOrganizationLocation = locationFlows.createLocation(otherOrganizationId);
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(locationAdminToken, organizationId, otherOrganizationLocation.getString("id"), groupCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3339", requirement = "PEG-2827")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void createGroupOnOtherOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject otherOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String otherOrganizationId = otherOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject otherOrganizationLocation = locationFlows.createLocation(otherOrganizationId);
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(token, otherOrganizationId, otherOrganizationLocation.getString("id"), groupCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3340", requirement = "PEG-2827")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void createGroupOnPausedOrganization(Role role) {
        final JSONObject pausedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        final String token = role.equals(SUPPORT) ? supportToken :
                authenticationFlowHelper.getTokenWithEmail(pausedOrganization.getJSONObject(role.name()).getString("email"));
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        final String locationId = pausedOrganization.getJSONObject("LOCATION").getString("id");

        LocationServiceGroupsHelper.createGroup(token, pausedOrganization.getJSONObject("ORGANIZATION").getString("id"),
                locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));
    }

    @Xray(test = "PEG-3341", requirement = "PEG-2827")
    @Test
    public void createGroupOnBlockedOrganization() {
        final JSONObject blockedOrganization = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String blockedOrganizationId = blockedOrganization.getJSONObject("ORGANIZATION").getString("id");

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        final String locationId = blockedOrganization.getJSONObject("LOCATION").getString("id");

        LocationServiceGroupsHelper.createGroup(supportToken, blockedOrganizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));

        final String ownerToken = blockedOrganization.getJSONObject(OWNER.name()).getString("token");
        LocationServiceGroupsHelper.createGroup(ownerToken, blockedOrganizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));

        final String adminToken = blockedOrganization.getJSONObject(ADMIN.name()).getString("token");
        LocationServiceGroupsHelper.createGroup(adminToken, blockedOrganizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));

        final String locationAdminToken = blockedOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        LocationServiceGroupsHelper.createGroup(locationAdminToken, blockedOrganizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));
    }

    @Xray(test = "PEG-3342", requirement = "PEG-2827")
    @Test
    public void createGroupOnUnpublishedOrganization() {
        final JSONObject unpublishedOrganization = organizationFlows.createUnpublishedOrganization();

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        final String unpublishedOrganizationLocationId = locationFlows.createLocation(unpublishedOrganization.getString("id")).getString("id");
        final String ownerToken = userFlows.createUser(unpublishedOrganization.getString("id"),
                OWNER, null).getString("token");
        final String adminToken = userFlows.createUser(unpublishedOrganization.getString("id"),
                ADMIN, null).getString("token");
        final String locationAdminToken = userFlows.createUser(unpublishedOrganization.getString("id"),
                LOCATION_ADMIN, Collections.singletonList(unpublishedOrganizationLocationId)).getString("token");

        LocationServiceGroupsHelper.createGroup(supportToken, unpublishedOrganization.getString("id"),
                unpublishedOrganizationLocationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(unpublishedOrganizationLocationId))
                .body("root", is(false));

        LocationServiceGroupsHelper.createGroup(ownerToken, unpublishedOrganization.getString("id"),
                unpublishedOrganizationLocationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(unpublishedOrganizationLocationId))
                .body("root", is(false));

        LocationServiceGroupsHelper.createGroup(adminToken, unpublishedOrganization.getString("id"),
                unpublishedOrganizationLocationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(unpublishedOrganizationLocationId))
                .body("root", is(false));

        LocationServiceGroupsHelper.createGroup(locationAdminToken, unpublishedOrganization.getString("id"),
                unpublishedOrganizationLocationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(unpublishedOrganizationLocationId))
                .body("root", is(false));
    }

    //    TODO known issue PEG-3659
    @Xray(test = "PEG-3343", requirement = "PEG-2827")
    @Test
    public void createGroupOnDeletedOrganization() {
        final JSONObject deletedOrganization = organizationFlows.createAndDeletePublishedOrganization();
        final String deletedOrganizationId = deletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = deletedOrganization.getJSONObject("LOCATION").getString("id");
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(supportToken, deletedOrganizationId, deletedOrganizationLocationId, groupCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3344", requirement = "PEG-2827")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void createGroupOnInactiveLocation(Role role) {
        final String inactivePhysicalLocation = locationFlows.createInactiveLocation(organizationId).getString("id");
        final JSONObject locationAdmin = userFlows.createUser(organizationId, LOCATION_ADMIN, Arrays.asList(inactivePhysicalLocation, inactivePhysicalLocation));
        final String token = role.equals(SUPPORT) ? supportToken
                : role.equals(LOCATION_ADMIN) ? authenticationFlowHelper.getTokenWithEmail(locationAdmin.getString("email"))
                : authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(token, organizationId, inactivePhysicalLocation, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(inactivePhysicalLocation))
                .body("root", is(false));
    }

    @Xray(test = "PEG-3335", requirement = "PEG-2827")
    @Test
    public void createGroupOnNonExistingOrganization() {
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(supportToken, UUID.randomUUID().toString(), UUID.randomUUID().toString(), groupCreationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-3346", requirement = "PEG-2827")
    @Test
    public void createGroupOnNonExistingLocationWithLocationAdmin() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(token, organizationId, UUID.randomUUID().toString(), groupCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3346", requirement = "PEG-2827")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void createGroupOnNonExistingLocation(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();

        LocationServiceGroupsHelper.createGroup(token, organizationId, UUID.randomUUID().toString(), groupCreationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-3347", requirement = "PEG-2827")
    @Test(testName = "PEG-3347", dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void createGroupWithNoName(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder().put(GroupCreationBody.NAME, "");
        LocationServiceGroupsHelper.createGroup(token, organizationId, UUID.randomUUID().toString(), groupCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-3348", requirement = "PEG-2827")
    @Test
    public void createGroupWithNoDisplayName() {
        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder().put(GroupCreationBody.DISPLAY_NAME, "");
        LocationServiceGroupsHelper.createGroup(supportToken, organizationId, UUID.randomUUID().toString(), groupCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    //TODO change Xrays and test - separate those
    @Xray(test = "PEG-3349, PEG-3350, PEG-3351", requirement = "PEG-2827")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void createManyLevelGroups(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final LocationServiceGroupFlows locationServiceGroupFlows = new LocationServiceGroupFlows();

        final String parentGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, null).getString("id");

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder().put(GroupCreationBody.PARENT_ID, parentGroupId);

        final String secondLevelGroupId = LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupCreationBody.getString("name")))
                .body("displayName", is(groupCreationBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("parentId", is(parentGroupId))
                .extract()
                .path("id");

        groupCreationBody.put(GroupCreationBody.PARENT_ID, secondLevelGroupId);

        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-3353", requirement = "PEG-2827")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void createGroupWithInactiveUser(Role role) {
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = user.getString("token");

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        userFlows.inactivateUserById(organizationId, user.getString("id"));

        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-3354", requirement = "PEG-2827")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void createGroupWithDeletedUser(Role role) {
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = user.getString("token");

        final JSONObject groupCreationBody = GroupCreationBody.bodyBuilder();
        userFlows.deleteUser(organizationId, user.getString("id"));

        LocationServiceGroupsHelper.createGroup(token, organizationId, locationId, groupCreationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

}
