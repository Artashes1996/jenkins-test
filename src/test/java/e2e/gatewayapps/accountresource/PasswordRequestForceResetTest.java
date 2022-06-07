package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.ForceResetPasswordRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.*;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;
import static org.hamcrest.Matchers.*;

public class PasswordRequestForceResetTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(test = "PEG-823", requirement = "PEG-477")
    @Test
    public void forceResetSupportBySupport() {
        final String email = getRandomInt() + "qa@qless.com";
        final String supportUserId = new SupportFlows().createSupport(email).getString("id");
        final String supportAccountId = DBHelper.getAccountIdByUserId(supportUserId);
        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, supportAccountId);

        AccountHelper.forceResetPasswordRequest(SUPPORT_TOKEN, organizationId, forceResetBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-879", requirement = "PEG-477")
    @Test
    public void forceResetByOwner() {
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final Role userRoleToReset = getRandomOrganizationRole();
        final String userId = userFlows.createUser(organizationId, userRoleToReset, Collections.singletonList(locationId)).getString("id");
        final String accountId = DBHelper.getAccountIdByUserId(userId);
        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);

        AccountHelper.forceResetPasswordRequest(ownerToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-874", requirement = "PEG-477")
    @Test
    public void forceResetBySupport() {
        final Role userRoleToReset = getRandomOrganizationRole();
        final String userId = userFlows.createUser(organizationId, userRoleToReset, Collections.singletonList(locationId)).getString("id");
        final String accountId = DBHelper.getAccountIdByUserId(userId);
        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);

        AccountHelper.forceResetPasswordRequest(SUPPORT_TOKEN, organizationId, forceResetBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-881", requirement = "PEG-477")
    @Test
    public void forceResetUserByAdmin() {
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");

        final JSONObject forceResetBody = new JSONObject();
        final Role randomRole = getRandomRolesWithLocation();
        final String userId = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId)).getString("id");
        final String locationAdminAccountId = DBHelper.getAccountIdByUserId(userId);
        forceResetBody.put(ForceResetPasswordRequestBody.ID, locationAdminAccountId);

        AccountHelper.forceResetPasswordRequest(adminToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-882", requirement = "PEG-477")
    @Test
    public void forceResetDeletedUser() {
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final Role randomRole = getRandomOrganizationRole();
        final String userId = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId)).getString("id");
        final String accountId = DBHelper.getAccountIdByUserId(userId);
        userFlows.deleteUser(organizationId, userId);

        final JSONObject forceResetBody = new JSONObject();
        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);

        AccountHelper.forceResetPasswordRequest(ownerToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-883", requirement = "PEG-477")
    @Test
    public void forceResetByStaff() {
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final Role randomRole = getRandomOrganizationRole();
        final String userId = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId)).getString("id");
        final String accountId = DBHelper.getAccountIdByUserId(userId);

        final JSONObject forceResetBody = new JSONObject();
        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);

        AccountHelper.forceResetPasswordRequest(staffToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-1733", requirement = "PEG-477")
    @Test
    public void forceResetOtherLocationStaffByLocationAdmin() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String otherLocationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String otherLocationUserId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(otherLocationId)).getString("id");
        final String otherLocationAccountId = DBHelper.getAccountIdByUserId(otherLocationUserId);
        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, otherLocationAccountId);

        AccountHelper.forceResetPasswordRequest(locationAdminToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-875", requirement = "PEG-477")
    @Test
    public void forceResetOtherOrganizationUser() {
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String otherOrganizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationOwnerUserId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject(OWNER.name()).getString("id");
        final String otherOrganizationOwnerAccountId = DBHelper.getAccountIdByUserId(otherOrganizationOwnerUserId);

        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, otherOrganizationOwnerAccountId);

        AccountHelper.forceResetPasswordRequest(ownerToken, otherOrganizationId, forceResetBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-878", requirement = "PEG-477")
    @Test
    public void forceResetDeletedOrganizationUser() {
        final JSONObject otherOrganization = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String otherOrganizationId = otherOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationOwnerUserId = otherOrganization.getJSONObject(OWNER.name()).getString("id");
        final String otherOrganizationOwnerAccountId = DBHelper.getAccountIdByUserId(otherOrganizationOwnerUserId);

        organizationFlows.deleteOrganization(otherOrganizationId);

        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, otherOrganizationOwnerAccountId);

        AccountHelper.forceResetPasswordRequest(SUPPORT_TOKEN, otherOrganizationId, forceResetBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-886", requirement = "PEG-477")
    @Test
    public void forceResetUpperLevelRole() {
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");

        final String ownerAccountId = DBHelper.getAccountIdByUserId(organizationAndUsers.getJSONObject(OWNER.name()).getString("id"));

        final JSONObject forceResetBody = new JSONObject();

        forceResetBody.put(ForceResetPasswordRequestBody.ID, ownerAccountId);

        AccountHelper.forceResetPasswordRequest(adminToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_FORBIDDEN);

        final String adminAccountId = DBHelper.getAccountIdByUserId(organizationAndUsers.getJSONObject(ADMIN.name()).getString("id"));

        forceResetBody.put(ForceResetPasswordRequestBody.ID, adminAccountId);

        AccountHelper.forceResetPasswordRequest(adminToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }


    @SneakyThrows
    @Xray(requirement = "PEG-4707", test = "PEG-5251")
    @Test
    public void oldTokenIsInvalidInCaseOfForceResetPassword() {

        final JSONObject forceResetBody = new JSONObject();
        final JSONObject userObject = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String userId = userObject.getString("id");
        final String userToken = userObject.getString("token");
        Thread.sleep(5000);
        final String accountId = DBHelper.getAccountIdByUserId(userId);
        forceResetBody.put(ForceResetPasswordRequestBody.ID, accountId);
        AccountHelper.forceResetPasswordRequest(userToken, organizationId, forceResetBody)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(userToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED);

    }
}
