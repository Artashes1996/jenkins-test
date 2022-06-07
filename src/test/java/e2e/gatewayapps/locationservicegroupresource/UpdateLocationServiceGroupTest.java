package e2e.gatewayapps.locationservicegroupresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationservicegroupsresource.LocationServiceGroupsHelper;
import helpers.appsapi.locationservicegroupsresource.payloads.GroupCreationBody;
import helpers.appsapi.locationservicegroupsresource.payloads.GroupUpdateBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static configuration.Role.SUPPORT;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;

public class UpdateLocationServiceGroupTest extends BaseTest {
    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;
    private LocationServiceGroupFlows locationServiceGroupFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;
    private String supportToken;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        locationServiceGroupFlows = new LocationServiceGroupFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        supportToken = getToken(SUPPORT);

    }

    @Xray(test = "PEG-4533", requirement = "PEG-3185")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void updateGroupBySupportedRoles(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken :
                organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, null).getString("id");
        LocationServiceGroupsHelper.updateGroup(token, organizationId, locationId, locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupUpdateBody.getString("name")))
                .body("displayName", is(groupUpdateBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));
    }

    @Xray(test = "PEG-4539", requirement = "PEG-3185")
    @Test
    public void updateSecondLevelOfGroup() {
        final String token = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, null, 2).getJSONObject("CHILD").getString("id");
        LocationServiceGroupsHelper.updateGroup(token, organizationId, locationId, locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createGroup.json"))
                .body("name", is(groupUpdateBody.getString("name")))
                .body("displayName", is(groupUpdateBody.getString("displayName")))
                .body("locationId", is(locationId))
                .body("root", is(false));
    }

    @Xray(test = "PEG-4540", requirement = "PEG-3185")
    @Test
    public void updateGroupByStaff() {
        final String token = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, null).getString("id");

        LocationServiceGroupsHelper.updateGroup(token, organizationId, locationId, locationServiceGroupId, GroupCreationBody.bodyBuilder())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-4541", requirement = "PEG-3185")
    @Test
    public void updateGroupWithLocationAdminOfOtherLocation() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String otherLocationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(organizationId, otherLocationId, null).getString("id");

        LocationServiceGroupsHelper.updateGroup(locationAdminToken, organizationId, otherLocationId, locationServiceGroupId, GroupCreationBody.bodyBuilder())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-4542", requirement = "PEG-3185")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void updateGroupOfOtherOrganizationLocation(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String otherOrganizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationLocation = new LocationFlows().createLocation(otherOrganizationId).getString("id");
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(otherOrganizationId, otherOrganizationLocation, null).getString("id");

        LocationServiceGroupsHelper.updateGroup(token, organizationId, otherOrganizationLocation, locationServiceGroupId, GroupUpdateBody.bodyBuilder())
                .then()
                .statusCode(SC_NOT_FOUND).
                body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-4543", requirement = "PEG-3185")
    @Test
    public void updateGroupOfOtherOrganizationLocationByLocationAdmin() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String otherOrganizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationLocationId = new LocationFlows().createLocation(otherOrganizationId).getString("id");
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(otherOrganizationId, otherOrganizationLocationId, null).getString("id");

        LocationServiceGroupsHelper.updateGroup(locationAdminToken, organizationId, otherOrganizationLocationId, locationServiceGroupId, GroupUpdateBody.bodyBuilder())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-4544", requirement = "PEG-3185")
    @Test
    public void updateGroupOnDeletedOrganization() {

        final JSONObject toBeDeletedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        final String deletedOrganizationId = toBeDeletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = toBeDeletedOrganization.getJSONObject("LOCATION").getString("id");
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(deletedOrganizationId, deletedOrganizationLocationId, null).getString("id");
        organizationFlows.deleteOrganization(deletedOrganizationId);
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();

        LocationServiceGroupsHelper.updateGroup(supportToken, deletedOrganizationId, deletedOrganizationLocationId, locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }


    @Xray(test = "PEG-4545", requirement = "PEG-3185")
    @Test
    public void updateGroupOnNonExistingOrganization() {
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(organizationId, locationId, null).getString("id");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();

        LocationServiceGroupsHelper.updateGroup(supportToken, UUID.randomUUID().toString(), locationId, locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-4546", requirement = "PEG-3185")
    @Test
    public void updateGroupOnNonExistingLocationWithLocationAdmin() {
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(organizationId, locationId, null).getString("id");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();

        LocationServiceGroupsHelper.updateGroup(locationAdminToken, organizationId, UUID.randomUUID().toString(), locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-4547", requirement = "PEG-3185")
    @Test
    public void updateGroupOnNonExistingLocation() {
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(organizationId, locationId, null).getString("id");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();

        LocationServiceGroupsHelper.updateGroup(supportToken, organizationId, UUID.randomUUID().toString(), locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-4548", requirement = "PEG-3185")
    @Test
    public void updateGroupWithNoName() {
        final String token = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(organizationId, locationId, null).getString("id");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder().put(GroupUpdateBody.NAME, "");

        LocationServiceGroupsHelper.updateGroup(token, organizationId, UUID.randomUUID().toString(), locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-4549", requirement = "PEG-3185")
    @Test
    public void updateGroupWithoutDisplayName() {
        final String token = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String locationServiceGroupId = new LocationServiceGroupFlows().createGroup(organizationId, locationId, null).getString("id");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder().put(GroupUpdateBody.DISPLAY_NAME, JSONObject.NULL);

        LocationServiceGroupsHelper.updateGroup(token, organizationId, UUID.randomUUID().toString(), locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-4550", requirement = "PEG-3185")
    @Test
    public void updateGroupWithInactiveUser() {
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject user = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String token = user.getString("token");
        final String locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, null).getString("id");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();

        userFlows.inactivateUserById(organizationId, user.getString("id"));

        LocationServiceGroupsHelper.updateGroup(token, organizationId, locationId, locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-4551", requirement = "PEG-3185")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void updateGroupWithDeletedUser(Role role) {
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = user.getString("token");
        final String locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, locationId, null).getString("id");
        final JSONObject groupUpdateBody = GroupUpdateBody.bodyBuilder();

        userFlows.deleteUser(organizationId, user.getString("id"));

        LocationServiceGroupsHelper.updateGroup(token, organizationId, locationId, locationServiceGroupId, groupUpdateBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}
