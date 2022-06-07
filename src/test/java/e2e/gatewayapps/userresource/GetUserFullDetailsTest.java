package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class GetUserFullDetailsTest extends BaseTest {

    private JSONObject organizationAndUsers;
    private String organizationId;
    private OrganizationFlows organizationFlows;
    private final AuthenticationFlowHelper authenticationFlowHelper = new AuthenticationFlowHelper();
    private final UserFlows userFlows = new UserFlows();
    private List<String> locationIds;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
    }

    @Test(testName = "PEG-2851, PEG-2852, PEG-2853, PEG-2854", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getById(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        Role randomRole = STAFF;
        if (role.equals(SUPPORT)) {
            randomRole = Role.values()[getRandomInt(values().length - 2) + 1];
        }

        final JSONObject user = userFlows.createUser(organizationId, randomRole, locationIds);

        UserHelper.getUserFullDetailsById(token, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("userStatus", equalTo(user.getString("status")))
                .body("id", equalTo(user.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));
    }

    @Test(testName = "PEG-2857, PEG-2858, PEG-2859, PEG-2860", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getSelfById(Role role) {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject user = organizationAndUsers.getJSONObject(role.name());

        UserHelper.getUserFullDetailsById(token, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("userStatus", equalTo(user.getString("status")))
                .body("id", equalTo(user.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));
    }

    @Test(testName = "PEG-2861", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getIncitedButNotAcceptedUserFullDetails(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        Role randomRole = STAFF;
        if (role.equals(SUPPORT) || role.equals(OWNER)) {
            randomRole = Role.values()[getRandomInt(values().length - 2) + 1];
        } else if (role.equals(ADMIN)) {
            randomRole = Role.values()[getRandomInt(values().length - 4) + 3];
        }

        final JSONObject user = userFlows.inviteUser(organizationId, randomRole, locationIds);
        final String userId = DBHelper.getUserIdByEmail(user.getString("email"));
        UserHelper.getUserFullDetailsById(token, organizationId, userId)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("invitationStatus", equalTo(user.getString("status")))
                .body("id", equalTo(userId))
                .body("accountId", equalTo(null));
    }

    @Test(testName = "PEG-2862", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getInactiveUserFullDetail(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        Role randomRole = STAFF;
        if (role.equals(SUPPORT) || role.equals(OWNER)) {
            randomRole = Role.values()[getRandomInt(values().length - 2) + 1];
        } else if (role.equals(ADMIN)) {
            randomRole = Role.values()[getRandomInt(values().length - 4) + 3];
        }
        final JSONObject user = userFlows.createUser(organizationId, randomRole, locationIds);
        userFlows.inactivateUserById(organizationId, user.getString("id"));

        UserHelper.getUserFullDetailsById(token, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("userStatus", equalTo("INACTIVE"))
                .body("id", equalTo(user.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));
    }

    @Test(testName = "PEG-2863", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getDeletedUserFullDetail(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        Role randomRole = STAFF;
        if (role.equals(SUPPORT) || role.equals(OWNER)) {
            randomRole = Role.values()[getRandomInt(values().length - 2) + 1];
        } else if (role.equals(ADMIN)) {
            randomRole = Role.values()[getRandomInt(values().length - 4) + 3];
        }
        final JSONObject user = userFlows.createUser(organizationId, randomRole, locationIds);
        userFlows.deleteUser(organizationId, user.getString("id"));

        UserHelper.getUserFullDetailsById(token, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("userStatus", equalTo("ACTIVE"))
                .body("id", equalTo(user.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"))
                .body("deleted", equalTo(true));
    }

    //    TODO issue, should be fixed and added proper XRAY
    @Test(testName = "PEG-2864")
    public void getOtherOrganizationUser() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        final JSONObject otherOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = otherOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationOwnerId = otherOrganizationAndUsers.getJSONObject(OWNER.name()).getString("id");

        UserHelper.getUserFullDetailsById(token, organizationId, otherOrganizationOwnerId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    //TODO issue should be fixed and fix XRAY
    @Test(testName = "PEG-2865")
    public void getStaffOfOneLocationByLocationAdminOfOtherLocation() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final String locationIds = new LocationFlows().createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationIds));

        UserHelper.getUserFullDetailsById(token, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("userStatus", equalTo("ACTIVE"))
                .body("id", equalTo(user.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));
    }

    @Test(testName = "PEG-2871", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getPausedOrganizationId(Role role) {
        final JSONObject pausedOrganizationAndOwners = organizationFlows.createAndPublishOrganizationWithAllUsers();

        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(pausedOrganizationAndOwners.getJSONObject(role.name()).getString("email"));

        final String pausedOrganizationId = pausedOrganizationAndOwners.getJSONObject("ORGANIZATION").getString("id");
        organizationFlows.pauseOrganization(pausedOrganizationId);

        final Role randomRole = Role.values()[(values().length - 2) + 1];
        final JSONObject user = pausedOrganizationAndOwners.getJSONObject(randomRole.name());

        UserHelper.getUserFullDetailsById(token, pausedOrganizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(user.getString("email")))
                .body("organizationId", equalTo(user.getString("organizationId")))
                .body("userStatus", equalTo("ACTIVE"))
                .body("id", equalTo(user.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));
    }

    @Test(testName = "PEG-2870, PEG-2869, PEG-2866", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getBlockedOrganizationId(Role role) {
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(blockedOrganizationAndUsers.getJSONObject(role.name()).getString("email"));

        final String blockedOrganizationId = blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        organizationFlows.blockOrganization(blockedOrganizationId);

        final Role randomRole = Role.values()[(values().length - 2) + 1];
        final JSONObject blockedOrganizationUser = blockedOrganizationAndUsers.getJSONObject(randomRole.name());

        UserHelper.getUserFullDetailsById(token, blockedOrganizationId, blockedOrganizationUser.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(blockedOrganizationUser.getString("email")))
                .body("organizationId", equalTo(blockedOrganizationUser.getString("organizationId")))
                .body("userStatus", equalTo("ACTIVE"))
                .body("id", equalTo(blockedOrganizationUser.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));

        UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, organizationId, blockedOrganizationUser.getString("id"))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));

        organizationFlows.deleteOrganization(blockedOrganizationId);

        UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, blockedOrganizationId, blockedOrganizationUser.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getUserFullDetailsById.json"))
                .body("email", equalTo(blockedOrganizationUser.getString("email")))
                .body("organizationId", equalTo(blockedOrganizationUser.getString("organizationId")))
                .body("userStatus", equalTo("ACTIVE"))
                .body("id", equalTo(blockedOrganizationUser.getString("id")))
                .body("invitationStatus", equalTo("ACCEPTED"));

    }

    @Test(testName = "PEG-2868")
    public void getNonExistingOrganizationUser() {
        final Role randomRole = Role.values()[(values().length - 2) + 1];
        final JSONObject user = organizationAndUsers.getJSONObject(randomRole.name());

        UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, UUID.randomUUID().toString(), user.getString("id"))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-2867", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getNonExistingUser(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        UserHelper.getUserFullDetailsById(token, organizationId, UUID.randomUUID().toString())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }
}
