package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.RestoreApplyBody;
import helpers.appsapi.accountresource.payloads.RestoreRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static helpers.DBHelper.getUserDeletedState;
import static utils.DbWaitUtils.waitForUserStateToBe;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class AccountApplyRestoreTest {

    private String organizationId;
    private UserFlows userFlows;
    private final String passwordToRestore = "Ew!12345678";

    private OrganizationFlows organizationFlows;
    private String locationId;

    @BeforeClass
    public void setup() {
        userFlows = new UserFlows();
        organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(test = "PEG-717")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void restoreUser(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));
//        TODO check this
        waitForUserStateToBe(() -> getUserDeletedState(user.getString("id")), true);
        final String restoreToken = userFlows.requestRestore(organizationId, user);

        final JSONObject restoreApply = new JSONObject();
        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, restoreToken);
        restoreApply.put(RestoreApplyBody.PASSWORD, passwordToRestore);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @Xray(test = "PEG-782")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void restoreWithInvalidPassword(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));
        final String restoreToken = userFlows.requestRestore(organizationId, user);

        final JSONObject restoreApplyBody = new JSONObject();
        restoreApplyBody.put(RestoreApplyBody.RESTORE_TOKEN, restoreToken);
        restoreApplyBody.put(RestoreApplyBody.PASSWORD, UUID.randomUUID());
        AccountHelper.restoreApply(restoreApplyBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-781")
    @Test
    public void restoreWithInvalidToken() {
        final JSONObject restoreApplyBody = new JSONObject();
        restoreApplyBody.put(RestoreApplyBody.PASSWORD, "Qw!123456");
        restoreApplyBody.put(RestoreApplyBody.RESTORE_TOKEN, UUID.randomUUID());
        AccountHelper.restoreApply(restoreApplyBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Xray(test = "PEG-783, PEG-785")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void restoreUserWithOldPassword(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));
        final String restoreToken = userFlows.requestRestore(organizationId, user);

        final JSONObject restoreApply = new JSONObject();
        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, restoreToken);
        final String passwordExisting = "Qw!123456";
        restoreApply.put(RestoreApplyBody.PASSWORD, passwordExisting);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_PRECONDITION_FAILED);
    }

    @Xray(test = "PEG-797, PEG-798")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void restoreUsersExpiredToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String userId = user.getString("id");
        userFlows.deleteUser(organizationId, userId);
        final String restoreToken = userFlows.requestRestore(organizationId, user);
        DBHelper.expireRestoreToken(userId);

        final JSONObject restoreApply = new JSONObject();
        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, restoreToken);
        restoreApply.put(RestoreApplyBody.PASSWORD, passwordToRestore);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_GONE);
    }

    @Xray(test = "PEG-849")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void restoreAlreadyRestoredAccount(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));
        userFlows.restoreUser(organizationId, user);

        final JSONObject restoreBody = new JSONObject();
        restoreBody.put(RestoreRequestBody.EMAIL, user.getString("email"));
        UserHelper.restoreRequest(SUPPORT_TOKEN, organizationId, restoreBody)
                .then()
                .statusCode(HttpStatus.SC_PRECONDITION_FAILED);
    }

    //TODO to be changed test case
    //TODO add XRay test case
    @Test
    public void restoreUserOfDeletedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        userFlows.deleteUser(organizationId,  owner.getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(STAFF.name()).getString("id"));

        final String ownerRequestToken = userFlows.requestRestore(organizationId, owner);
        final String adminRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(ADMIN.name()));
        final String locationAdminRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()));
        final String staffRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(STAFF.name()));

        organizationFlows.deleteOrganization(organizationId);

        final JSONObject restoreApply = new JSONObject();
        restoreApply.put(RestoreApplyBody.PASSWORD, passwordToRestore);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, ownerRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, adminRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, locationAdminRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, staffRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // TODO add XRay
    @Test
    public void restoreUserOfBlockedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        userFlows.deleteUser(organizationId,  owner.getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(STAFF.name()).getString("id"));

        final String ownerRequestToken = userFlows.requestRestore(organizationId, owner);
        final String adminRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(ADMIN.name()));
        final String locationAdminRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()));
        final String staffRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(STAFF.name()));

        organizationFlows.blockOrganization(organizationId);

        final JSONObject restoreApply = new JSONObject();
        restoreApply.put(RestoreApplyBody.PASSWORD, passwordToRestore);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, ownerRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, adminRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, locationAdminRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, staffRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
    // TODO add XRay
    @Test
    public void restoreUserOfPausedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        userFlows.deleteUser(organizationId,  owner.getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        userFlows.deleteUser(organizationId,  organizationWithUsers.getJSONObject(STAFF.name()).getString("id"));

        final String ownerRequestToken = userFlows.requestRestore(organizationId, owner);
        final String adminRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(ADMIN.name()));
        final String locationAdminRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()));
        final String staffRequestToken = userFlows.requestRestore(organizationId, organizationWithUsers.getJSONObject(STAFF.name()));

        organizationFlows.pauseOrganization(organizationId);

        final JSONObject restoreApply = new JSONObject();
        restoreApply.put(RestoreApplyBody.PASSWORD, passwordToRestore);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, ownerRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, adminRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, locationAdminRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);

        restoreApply.put(RestoreApplyBody.RESTORE_TOKEN, staffRequestToken);
        AccountHelper.restoreApply(restoreApply)
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

}
