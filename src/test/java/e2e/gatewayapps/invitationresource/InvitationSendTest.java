package e2e.gatewayapps.invitationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.appsapi.invitationresource.InvitationHelper;
import helpers.appsapi.invitationresource.payloads.InvitationCreationBody;
import helpers.flows.UserFlows;
import org.apache.http.HttpStatus;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;
import static org.hamcrest.Matchers.*;

public class InvitationSendTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private String ownerToken;
    private final UserFlows userFlows = new UserFlows();
    private ArrayList<String> locationIds;
    private String inactiveLocationId;
    private AuthenticationFlowHelper authenticationFlowHelper;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.toString()).getString("email"));
        final LocationFlows locationFlows = new LocationFlows();
        locationIds = locationFlows.createLocationsAndReturnIdsOnly(organizationId, 3);
        inactiveLocationId = locationFlows.createInactiveLocation(organizationId).getString("id");
        authenticationFlowHelper = new AuthenticationFlowHelper();
    }

    // Test should be passed after fixed PEG-7215
    @Xray(test = "PEG-2482")
    @Test(dataProvider = "invalidRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteUserWithNonExistingRole(String role) {

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, STAFF, locationIds);

        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0)
                .getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).put(ROLE_INTERNAL_NAME, role);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-2483")
    @Test
    public void inviteUserToADeletedOrganization() {
        final String deletedOrganizationId = organizationFlows.createAndPublishOrganizationWithAllUsers()
                .getJSONObject("ORGANIZATION").getString("id");
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, OWNER, null);
        organizationFlows.deleteOrganization(deletedOrganizationId);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, deletedOrganizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-2484")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void inviteUserToABlockedOrganization(Role role) {
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String blockedOrganizationId = blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(blockedOrganizationAndUsers.getJSONObject(role.name()).getString("email"));
        organizationFlows.blockOrganization(blockedOrganizationId);

        final List<String> locationIds = Collections.singletonList(blockedOrganizationAndUsers.getJSONObject("LOCATION").getString("id"));
        Role randomRole = STAFF;
        if (role.equals(SUPPORT) || role.equals(OWNER)) {
            randomRole = Role.values()[getRandomInt(values().length - 1) + 1];
        } else if (role.equals(ADMIN)) {
            randomRole = Role.values()[getRandomInt(values().length - 3) + 3];
        }
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, randomRole, locationIds);
        InvitationHelper.inviteUsers(token, blockedOrganizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2485", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void inviteUserToAPausedOrganization(Role role) {
        final JSONObject pausedOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String pausedOrganizationId = pausedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(pausedOrganizationAndUsers.getJSONObject(role.name()).getString("email"));
        organizationFlows.pauseOrganization(pausedOrganizationId);
        Role randomRole = STAFF;
        if (role.equals(SUPPORT) || role.equals(OWNER)) {
            randomRole = Role.values()[getRandomInt(values().length - 1) + 1];
        } else if (role.equals(ADMIN)) {
            randomRole = Role.values()[getRandomInt(values().length - 3) + 3];
        }

        final List<String> locationIds = Collections.singletonList(pausedOrganizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, randomRole, locationIds);
        InvitationHelper.inviteUsers(token, pausedOrganizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2486", dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteUsersToUnpublishedOrganization(Role role) {
        final JSONObject unpublishedOrganization = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String unpublishedOrganizationId = unpublishedOrganization.getJSONObject("ORGANIZATION").getString("id");

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, role, null);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, unpublishedOrganizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2487", dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void inviteUserToInactiveLocation(Role role) {

        final List<String> locationIds = Collections.singletonList(inactiveLocationId);

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, role, locationIds);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2492, PEG-2493", dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteAdminAndOwnerByAdmin(Role role) {
        final String adminToken = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.toString()).getString("email"));
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, role, null);
        InvitationHelper.inviteUsers(adminToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-2494")
    public void inviteLocAdminByLocAdmin() {
        final String locationAdminToken = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, LOCATION_ADMIN, locationIds);
        InvitationHelper.inviteUsers(locationAdminToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-2495")
    public void inviteAdminByLocationAdmin() {
        final String locationAdminToken = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.toString()).getString("email"));

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, ADMIN, null);
        InvitationHelper.inviteUsers(locationAdminToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-2496")
    public void inviteOwnerByLocationAdmin() {
        final String locationAdminToken = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.toString()).getString("email"));

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, OWNER, null);
        InvitationHelper.inviteUsers(locationAdminToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-2497, PEG-2498, PEG-2499, PEG-2501", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteByStaff(Role role) {
        final String staffToken = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.toString()).getString("email"));

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds);
        InvitationHelper.inviteUsers(staffToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    //TODO check this, xray as well
    @Test(testName = "PEG-2502")
    public void inviteWithInactiveOwner() {
        final JSONObject inactiveOwner = userFlows.createUser(organizationId, OWNER, null);
        final String inactiveOwnerToken = authenticationFlowHelper.getTokenWithEmail(inactiveOwner.getString("email"));
        userFlows.inactivateUserById(organizationId, inactiveOwner.getString("id"));
        final Role randomRole = Role.values()[getRandomInt(values().length)];
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, randomRole, locationIds);
        InvitationHelper.inviteUsers(inactiveOwnerToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    //TODO check this, xray as well
    @Test(testName = "PEG-2503")
    public void inviteWithDeletedOwner() {
        final JSONObject deletedOwner = userFlows.createUser(organizationId, OWNER, null);
        final String deletedOwnerToken = authenticationFlowHelper.getTokenWithEmail(deletedOwner.getString("email"));
        userFlows.deleteUser(organizationId, deletedOwner.getString("id"));
        final Role randomRole = Role.values()[getRandomInt(values().length)];
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.REQUIRED, randomRole, locationIds);
        InvitationHelper.inviteUsers(deletedOwnerToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test(testName = "PEG-2461, PEG-2462, PEG-2463, PEG-2464, ", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void inviteStaff(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.toString()).getString("email"));
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, STAFF, locationIds);

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("INVITATION_CREATION_ISSUE_DETECTED"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    //  Test should be passed after fixed PEG-7215
    @Test(testName = "PEG-2465")
    public void inviteWithoutRole() {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, OWNER, null);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).remove(ROLE_INTERNAL_NAME);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-2468")
    public void inviteLocationAdminWithoutLocationID() {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, OWNER, null);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getJSONArray(ROLE_LOCATION_PAYLOADS)
                .getJSONObject(0).put(ROLE_INTERNAL_NAME, LOCATION_ADMIN);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    //TODO check this
    @Test(testName = "PEG-2469")
    public void inviteStaffWithoutLocationID() {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, OWNER, null);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getJSONArray(ROLE_LOCATION_PAYLOADS)
                .getJSONObject(0).put(ROLE_INTERNAL_NAME, STAFF);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2470")
    public void inviteWithoutRoleLocationPayload() {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, STAFF, locationIds);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).remove(ROLE_LOCATION_PAYLOADS);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

//  TODO Invalid Format
    @Test(testName = "PEG-2471", dataProvider = "invalidEmail", dataProviderClass = InvitationDataProvider.class)
    public void inviteWithWrongFormattedEmail(Object email) {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds);
        invitationBody.getJSONArray("payloads").getJSONObject(0).put(EMAIL, email);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2471")
    public void inviteWithWrongFormattedEmailType() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds);
        invitationBody.getJSONArray("payloads").getJSONObject(0).put(EMAIL, -1);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Test(testName = "PEG-2472", dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void inviteUserWithOtherOrganizationLocationId(Role role) {
        final JSONObject otherOrganizationAndLocation = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final List<String> locationIds = Collections.singletonList(otherOrganizationAndLocation.getJSONObject("LOCATION").getString("id"));
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-2473", dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void inviteUserWithNonExistingLocationId(Role role) {
        final List<String> locationIds = Collections.singletonList(UUID.randomUUID().toString());
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-2473")
    public void inviteUserWithNonExistingOrganizationId() {
        final Role randomRole = Role.values()[getRandomInt(values().length)];
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, UUID.randomUUID().toString(), invitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    // TODO check this
    @Test(enabled = false, testName = "PEG-2475")
    public void inviteOtherOrgsExistingUserByOwner() {
        final String otherOrganizationEmail = organizationFlows.createAndPublishOrganizationWithOwner()
                .getJSONObject(OWNER.name()).getString("email");
        final Role randomRole = Role.values()[getRandomInt(values().length)];
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds);
        final JSONObject payloads = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0);
        payloads.put(EMAIL, otherOrganizationEmail);
        invitationBody.put(PAYLOADS, new JSONArray().put(payloads));

        InvitationHelper.inviteUsers(ownerToken, organizationId, invitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    //    TODO check this and fix xray
    @Test(enabled = false, testName = "PEG-2475")
    public void inviteOtherOrgsExistingUserBySupport() {
        final String otherOrganizationEmail = organizationFlows.createAndPublishOrganizationWithOwner()
                .getJSONObject(OWNER.toString()).getString("email");
        final Role randomRole = Role.values()[getRandomInt(values().length)];
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds);
        final JSONObject payloads = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0);
        payloads.put(EMAIL, otherOrganizationEmail);
        invitationBody.put(PAYLOADS, new JSONArray().put(payloads));

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-2477", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteUserWithDifferentRolesOnTheSameLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;

        final List<String> locationId = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, STAFF, locationId);
        final JSONObject roleLocation = new JSONObject();
        roleLocation.put(ROLE_INTERNAL_NAME, LOCATION_ADMIN);
        roleLocation.put(LOCATION_ID, locationId.get(0));
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getJSONArray(ROLE_LOCATION_PAYLOADS).put(roleLocation);

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2480", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteWithEmptyBody(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        InvitationHelper.inviteUsers(token, organizationId, new JSONObject())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2481", dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void inviteUserWithBadFormatterPhoneNumber(Object invalidPhone) {
        final Role randomRole = Role.getRandomOrganizationRole();
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).put(CONTACT_NUMBER, invalidPhone);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2454, PEG-2455", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteOwnerWithSupportedUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, OWNER, locationIds);

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("INVITATION_CREATION_ISSUE_DETECTED"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2456, PEg-2457", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteAdminWithSupportedRoles(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, ADMIN, locationIds);

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CONFLICT);

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2458, PEG-2459", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteLocationAdminBySupportedRoles(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, LOCATION_ADMIN, locationIds);

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("INVITATION_CREATION_ISSUE_DETECTED"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(testName = "PEG-2460")
    public void inviteLocationAdminByAdmin() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.toString()).getString("email"));

        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, LOCATION_ADMIN, locationIds);

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("INVITATION_CREATION_ISSUE_DETECTED"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test(dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteOwnerAndAdminWithMinimumRequiredFields(Role role) {
        final JSONObject invitationBody = new JSONObject();
        final JSONArray payloads = new JSONArray();
        final JSONObject payload = new JSONObject();
        final JSONArray roleLocationPayloads = new JSONArray();
        final JSONObject roleLocationPayload = new JSONObject();
        roleLocationPayload.put(ROLE_INTERNAL_NAME, role);
        roleLocationPayloads.put(roleLocationPayload);
        final String email = getRandomInt() + "@qless.qa";
        payload.put(ROLE_LOCATION_PAYLOADS, roleLocationPayloads);
        payload.put(EMAIL, email);
        payloads.put(payload);
        invitationBody.put(PAYLOADS, payloads);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(EMAIL, hasItem(email))
                .body(ORGANIZATION_ID, hasItem(organizationId))
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

    @Test
    public void inviteOrganizationLevelPointOfContactWithoutContact() {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, OWNER, null);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).remove(CONTACT_NUMBER);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-297", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void inviteWithoutEmail(Role role) {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds);
        invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).remove(EMAIL);
        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-295", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void inviteByValidAndInvalidEmails(Role role) {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds);
        final JSONObject secondPayloadWithInvalidEmail = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, role, locationIds).getJSONArray(PAYLOADS).getJSONObject(0);
        secondPayloadWithInvalidEmail.put(EMAIL, UUID.randomUUID().toString());
        invitationBody.getJSONArray(PAYLOADS).put(secondPayloadWithInvalidEmail);

        InvitationHelper.inviteUsers(SUPPORT_TOKEN, organizationId, invitationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-", dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void invite20UsersAtOnce(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, STAFF, locationIds);

        for (int i = 0; i < 20; i++) {
            Role randomRole = STAFF;
            if (role.equals(SUPPORT) || role.equals(OWNER)) {
                randomRole = Role.values()[getRandomInt(values().length - 1) + 1];
            } else if (role.equals(ADMIN)) {
                randomRole = Role.values()[getRandomInt(values().length - 3) + 3];
            }
            final JSONObject secondPayloadWithInvalidEmail = InvitationCreationBody.bodyBuilder(InvitationCreateCombination.ALL_FIELDS, randomRole, locationIds).getJSONArray(PAYLOADS).getJSONObject(0);
            invitationBody.getJSONArray(PAYLOADS).put(secondPayloadWithInvalidEmail);
        }

        InvitationHelper.inviteUsers(token, organizationId, invitationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/invitation.json"));

        final String email = invitationBody.getJSONArray(PAYLOADS).getJSONObject(0).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();

        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", equalTo(email));
    }

}