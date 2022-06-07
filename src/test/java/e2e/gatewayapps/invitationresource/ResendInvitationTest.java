package e2e.gatewayapps.invitationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.*;
import org.testng.annotations.*;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.invitationresource.InvitationHelper.resendInvitation;
import static helpers.appsapi.invitationresource.payloads.ResendInvitationBody.EMAILS;
import static helpers.appsapi.invitationresource.payloads.ResendInvitationBody.ORGANIZATION_ID;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;

public class ResendInvitationTest extends BaseTest {

    private String organizationId;
    private JSONObject organizationAndUsers;
    private final UserFlows userFlows = new UserFlows();
    private OrganizationFlows organizationFlows;
    private AuthenticationFlowHelper authenticationFlowHelper;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        authenticationFlowHelper = new AuthenticationFlowHelper();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @Test(testName = "PEG-560, PEG-608", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendPendingInvites(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final JSONArray emails = new JSONArray();
        for (int i = 0; i < 20; i++) {
            Role randomRole = STAFF;
            if (role.equals(SUPPORT) || role.equals(OWNER)) {
                randomRole = Role.values()[getRandomInt(values().length - 1) + 1];
            } else if (role.equals(ADMIN)) {
                randomRole = Role.values()[getRandomInt(values().length - 3) + 3];
            }
            emails.put(userFlows.inviteUser(organizationId, randomRole, locationIds).getString("email"));
        }

        final JSONObject resendInvitationBody = new JSONObject();

        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-600, PEG-607", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendExpiredInvites(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final JSONArray emails = new JSONArray();
        for (int i = 0; i < 20; i++) {
            Role randomRole = STAFF;
            if (role.equals(SUPPORT) || role.equals(OWNER)) {
                randomRole = Role.values()[getRandomInt(values().length - 2) + 1];
            } else if (role.equals(ADMIN)) {
                randomRole = Role.values()[getRandomInt(values().length - 4) + 3];
            }
            final JSONObject user = userFlows.inviteUser(organizationId, randomRole, locationIds);
            emails.put(user.getString("email"));
            DBHelper.expireInvitationToken(user.getString("token"));
        }

        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        if (role.equals(SUPPORT)) resendInvitationBody.put(ORGANIZATION_ID, organizationId);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-622, PEG-648", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendToAccepted(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final List<String> locationId = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final String email = userFlows.createUser(organizationId, STAFF, locationId).getString("email");

        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, new JSONArray().put(email));
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_GONE)
                .body("type", is("RESOURCE_NOT_VIABLE"));
    }

    @Test(testName = "PEG-649, PEG-650", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendInvitationToNotInvitedUser(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONArray nonInvitedUserEmail = new JSONArray();
        nonInvitedUserEmail.put(RANDOM.nextInt() + "@qless.qa");
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, nonInvitedUserEmail);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-641", dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void resendInvitationWithOtherOrganizationId(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject otherOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String otherOrganizationId = otherOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final List<String> otherOrganizationLocationId = Collections.singletonList(otherOrganizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final String email = userFlows.inviteUser(otherOrganizationId, STAFF, otherOrganizationLocationId).getString("email");

        final JSONArray emails = new JSONArray();
        emails.put(email);
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitationBody.put(ORGANIZATION_ID, otherOrganizationId);

        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-673", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendToDeletedUser(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject user = userFlows.inviteUser(organizationId, STAFF, locationIds);

        final JSONArray emails = new JSONArray();
        emails.put(user.getString("email"));

        final String deletedUserId = DBHelper.getUserIdByEmail(user.getString("email"));

        userFlows.deleteUser(organizationId, deletedUserId);
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-2836", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendInvitationInBulkIncludingAccepted(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final JSONArray emails = new JSONArray();
        emails.put(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        for (int i = 0; i < 20; i++) {
            Role randomRole = STAFF;
            if (role.equals(SUPPORT) || role.equals(OWNER)) {
                randomRole = Role.values()[getRandomInt(values().length - 1) + 1];
            } else if (role.equals(ADMIN)) {
                randomRole = Role.values()[getRandomInt(values().length - 3) + 3];
            }
            final JSONObject user = userFlows.inviteUser(organizationId, randomRole, locationIds);
            emails.put(user.getString("email"));
        }

        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_GONE)
                .body("type", is("RESOURCE_NOT_VIABLE"));
    }

    @Test(testName = "PEG-2837", dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void resendInvitationInBulkIncludingDeleted(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        final JSONArray emails = new JSONArray();
        emails.put(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        for (int i = 0; i < 20; i++) {
            Role randomRole = STAFF;
            if (role.equals(SUPPORT) || role.equals(OWNER)) {
                randomRole = Role.values()[getRandomInt(values().length - 2) + 1];
            } else if (role.equals(ADMIN)) {
                randomRole = Role.values()[getRandomInt(values().length - 3) + 3];
            }
            final JSONObject user = userFlows.inviteUser(organizationId, randomRole, locationIds);
            emails.put(user.getString("email"));
        }

        userFlows.deleteUser(organizationId, DBHelper.getUserIdByEmail(emails.get(1).toString()));

        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    //    TODO check the status code after fixing the case with ADMIN, and write into the XRAY
    @Test(testName = "PEG-2838", dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void resendInvitationByAdminIncludingOwnerOrAdmin(Role role) {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        final JSONArray emails = new JSONArray();
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject staffUser = userFlows.inviteUser(organizationId, STAFF, locationIds);
        final JSONObject ownerUser = userFlows.inviteUser(organizationId, role, locationIds);
        emails.put(staffUser.getString("email"));
        emails.put(ownerUser.getString("email"));
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    //TODO issue, fix and write in XRAY
    @Test(testName = "PEG-2840, PEG-2841", dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void resendInvitationByLocationAdminIncludingOwnerAdmin(Role role) {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final JSONArray emails = new JSONArray();
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject staffUser = userFlows.inviteUser(organizationId, STAFF, locationIds);
        final JSONObject unsupportedUser = userFlows.inviteUser(organizationId, role, locationIds);
        emails.put(staffUser.getString("email"));
        emails.put(unsupportedUser.getString("email"));
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-2840, PEG-2841")
    public void resendInvitationByLocationAdminIncludingLocationAdmin() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final JSONArray emails = new JSONArray();
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject staffUser = userFlows.inviteUser(organizationId, STAFF, locationIds);
        final JSONObject locationAdminUser = userFlows.inviteUser(organizationId, LOCATION_ADMIN, locationIds);
        emails.put(staffUser.getString("email"));
        emails.put(locationAdminUser.getString("email"));
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-2843")
    public void resendInvitationByStaff() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        final JSONArray emails = new JSONArray();
        final List<String> locationIds = Collections.singletonList(organizationAndUsers.getJSONObject("LOCATION").getString("id"));
        final JSONObject staffUser = userFlows.inviteUser(organizationId, STAFF, locationIds);
        emails.put(staffUser.getString("email"));
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEF-2844")
    public void resendInvitationByLocationAdminToOtherLocationStaff() {
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final JSONArray emails = new JSONArray();
        final List<String> locationIds = Collections.singletonList(new LocationFlows().createInactiveLocation(organizationId).getString("id"));
        final JSONObject staffUser = userFlows.inviteUser(organizationId, STAFF, locationIds);
        emails.put(staffUser.getString("email"));
        final JSONObject resendInvitationBody = new JSONObject();
        resendInvitationBody.put(EMAILS, emails);
        resendInvitation(token, organizationId, resendInvitationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }
}
