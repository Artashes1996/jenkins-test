package e2e.gatewayapps.accountresource;


import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.accountresource.data.AccountDetailsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.SupportFlows;
import helpers.flows.UserFlows;
import helpers.appsapi.accountresource.payloads.RestoreRequestBody;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;
import java.util.Random;

import static configuration.Role.*;
import static org.hamcrest.Matchers.equalTo;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;
import static org.testng.Assert.*;


public class AccountRestoreRequestTest extends BaseTest {

    private String organizationId;
    private UserFlows userFlows;

    private OrganizationFlows organizationFlows;
    private String locationId;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void setup() {
        userFlows = new UserFlows();
        organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
    }

    @Xray(test = "PEG-717")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void requestRestoreUserBySupport(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Xray(test = "PEG-786, PEG-710")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void requestRestoreUserByOwner(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(ownerToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Xray(test = "PEG-711")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "rolesWithLocation")
    public void requestRestoreUserByAdmin(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(adminToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    // TODO add XRay
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void requestRestoreUnsupportedUserByAdmin(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(adminToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    // TODO add XRay
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void requestRestoreUnsupportedUserByLocationAdmin(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(locationAdminToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    // TODO add XRay
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void requestRestoreUnsupportedUserByStaff(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(staffToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Xray(test = "PEG-847")
    @Test
    public void requestRestoreForActiveAccount() {
        final JSONObject admin = userFlows.createUser(organizationId, ADMIN, null);
        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, admin.getString("email"));

        UserHelper.restoreRequest(ownerToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_PRECONDITION_FAILED);
    }

    @Xray(test = "PEG-848")
    @Test
    public void requestRestoreForInactiveAccount() {
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);
        userFlows.inactivateUserById(organizationId, owner.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, owner.getString("email"));

        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_PRECONDITION_FAILED);
    }

    @Xray(test = "PEG-718")
    @Test(dataProvider = "invalidEmail", dataProviderClass = AccountDetailsDataProvider.class)
    public void requestRestoreWithInvalidEmail(Object email) {
        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, email);

        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("types[0]", equalTo("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-718")
    @Test
    public void requestRestoreWithInvalidBooleanEmail() {
        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, true);

        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("types[0]", equalTo("NOT_READABLE_REQUEST_BODY"));
    }


    @Xray(test = "PEG-719")
    @Test
    public void requestRestoreNonExistingAccount() {
        final String email = new Random().nextLong() + "qa@mail.qa";
        final JSONObject restoreBody = new JSONObject();

        restoreBody.put(RestoreRequestBody.EMAIL, email);
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("types[0]", equalTo("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1730")
    @Test
    public void requestRestoreDeletedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        final JSONObject restoreBody = new JSONObject();
        userFlows.deleteUser(organizationId,  owner.getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(STAFF.name()).getString("id"));

        organizationFlows.deleteOrganization(organizationId);

        restoreBody.put(RestoreRequestBody.EMAIL, owner.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(ADMIN.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(STAFF.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    // TODO add XRay test cases
    @Test
    public void requestRestoreBlockedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        final JSONObject restoreBody = new JSONObject();
        userFlows.deleteUser(organizationId,  owner.getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(STAFF.name()).getString("id"));

        organizationFlows.blockOrganization(organizationId);

        restoreBody.put(RestoreRequestBody.EMAIL, owner.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(ADMIN.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(STAFF.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    // TODO add XRay test cases
    @Test
    public void requestRestorePausedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        final JSONObject restoreBody = new JSONObject();
        userFlows.deleteUser(organizationId,  owner.getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(STAFF.name()).getString("id"));

        organizationFlows.pauseOrganization(organizationId);

        restoreBody.put(RestoreRequestBody.EMAIL, owner.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(ADMIN.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        restoreBody.put(RestoreRequestBody.EMAIL, organizationWithUsers.getJSONObject(STAFF.name()).getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    // TODO change it when everything is implemented in BE
    @Test(enabled = false)
    public void requestRestoreSupportByUnauthorizedUsers() {
        final String email = getRandomInt() + "qa@qless.com";
        final JSONObject supportUser = new SupportFlows().createSupport(email);
        new SupportFlows().deleteSupport(supportUser.getString("id"));

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, email);
        UserHelper.restoreRequest(ownerToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
        UserHelper.restoreRequest(locationAdminToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
        UserHelper.restoreRequest(adminToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
        UserHelper.restoreRequest(staffToken, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    // TODO add XRay test cases
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void invitedUserRestoreRequest(Role role) {
        final JSONObject user = userFlows.inviteUser(organizationId, role, Collections.singletonList(locationId));
        final String userId = userFlows.getUserId(user.getString("email"), organizationId);
        userFlows.deleteUser(organizationId, userId);

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_OK);
        assertEquals(DBHelper.getUserFieldValueById(userId, "deleted"), false);
    }
}