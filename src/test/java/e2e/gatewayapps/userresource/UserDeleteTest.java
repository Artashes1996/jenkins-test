package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.gatewayapps.userresource.data.UserDetailsDataProvider;
import helpers.DBHelper;
import helpers.appsapi.support.invitationresource.flows.SupportFlows;
import helpers.flows.UserFlows;
import helpers.flows.OrganizationFlows;
import helpers.appsapi.usersresource.UserHelper;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;

import static configuration.Role.*;
import static org.hamcrest.Matchers.equalTo;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class UserDeleteTest extends BaseTest {

    private String organizationId;
    private String locationId;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;
    private final UserFlows userFlows = new UserFlows();
    private OrganizationFlows organizationFlows;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        final JSONObject orgAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = orgAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = orgAndUsers.getJSONObject("LOCATION").getString("id");

        ownerToken = orgAndUsers.getJSONObject(OWNER.name()).getString("token");
        adminToken = orgAndUsers.getJSONObject(ADMIN.name()).getString("token");
        locationAdminToken = orgAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        staffToken = orgAndUsers.getJSONObject(STAFF.name()).getString("token");
    }

    @Test(testName = "PEG-638", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteWithOrganizationSupport(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-638", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteWithOrganizationOwner(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(ownerToken, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-638", dataProviderClass = RoleDataProvider.class, dataProvider = "rolesWithLocation")
    public void deleteWithOrganizationAdmin(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(adminToken, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-639", dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void deleteWithOrganizationAdminUnsupportedUsers(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(adminToken, organizationId, userId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-")
    public void deleteWithOrganizationLocationAdmin() {
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(adminToken, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-", dataProviderClass = RoleDataProvider.class, dataProvider = "organizationLevelInviters")
    public void deleteWithOrganizationLocationAdminUnsupportedUsers(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(locationAdminToken, organizationId, userId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteWithOrganizationStaffUnsupportedUsers(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        UserHelper.deleteUser(staffToken, organizationId, userId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-640", dataProvider = "invalidAccountId", dataProviderClass = UserDetailsDataProvider.class)
    public void deleteUserWithInvalidId(Object accountId) {
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, accountId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-642")
    public void deleteAnotherOrganizationUser() {
        final JSONObject organizationWithOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String userId = organizationWithOwner.getJSONObject(OWNER.name()).getString("id");
        final String organizationId = organizationWithOwner.getJSONObject("ORGANIZATION").getString("id");

        UserHelper.deleteUser(ownerToken, organizationId, userId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-643", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteDeletedUser(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        userFlows.deleteUser(organizationId, userId);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, userId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-644")
    public void deletePendingInvitation() {
        final JSONObject user = userFlows.inviteUser(organizationId, OWNER, null);
        final String userId = DBHelper.getUserIdByEmail(user.getString("email"));
        UserHelper.deleteUser(ownerToken, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-645")
    public void deleteExpiredInvitation() {
        final JSONObject user = userFlows.inviteUser(organizationId, OWNER, null);
        final String userId = DBHelper.getUserIdByEmail(user.getString("email"));
        DBHelper.expireInvitationToken(user.getString("token"));
        UserHelper.deleteUser(ownerToken, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-646", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteInactiveUser(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        userFlows.inactivateUserById(organizationId, userId);

        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-647")
    public void deleteSupportUser() {
        final String newSupportId = new SupportFlows().createSupport(SUPPORT_TOKEN, getRandomInt() + "@qless.com");

        UserHelper.deleteUser(ownerToken, organizationId, newSupportId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @SneakyThrows
    @Test(testName = "PEG-1744")
    public void deleteWithSupport() {
        final String userId = userFlows.createUser(organizationId, OWNER, null).getString("id");
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, userId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-1745")
    public void deleteUserOfDeletedOrganizationWithSupport() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String randomUserId = organizationAndOwner.getJSONObject(getRandomOrganizationRole().name()).getString("id");
        organizationFlows.deleteOrganization(organizationId);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, randomUserId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-1763")
    public void deleteAllOrganizationPOCs() {
        final JSONObject organizationAndOwner = new OrganizationFlows().createUnpublishedOrganizationWithOwner();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerId = organizationAndOwner.getJSONObject(OWNER.name()).getString("id");

        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, ownerId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-1765")
    public void deletePausedStateOrganizationPOC() {
        final JSONObject organizationAndOwner = organizationFlows.createPausedOrganizationWithAllUsers();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerId = organizationAndOwner.getJSONObject(OWNER.name()).getString("id");
        final String adminId = organizationAndOwner.getJSONObject(ADMIN.name()).getString("id");
        final String locationAdminId = organizationAndOwner.getJSONObject(LOCATION_ADMIN.name()).getString("id");
        final String staffId = organizationAndOwner.getJSONObject(STAFF.name()).getString("id");

        userFlows.createOwnerWithOrganizationIdWithoutPOC(organizationId);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, locationAdminId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, staffId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, adminId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, ownerId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Test(testName = "PEG-1768")
    public void deleteBlockedStateOrganizationPOC() {
        final JSONObject organizationAndOwner = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerId = organizationAndOwner.getJSONObject(OWNER.name()).getString("id");
        final String adminId = organizationAndOwner.getJSONObject(ADMIN.name()).getString("id");
        final String locationAdminId = organizationAndOwner.getJSONObject(LOCATION_ADMIN.name()).getString("id");
        final String staffId = organizationAndOwner.getJSONObject(STAFF.name()).getString("id");

        userFlows.createOwnerWithOrganizationIdWithoutPOC(organizationId);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, ownerId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, adminId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, locationAdminId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.deleteUser(SUPPORT_TOKEN, organizationId, staffId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Xray(test = "PEG-5254", requirement = "PEG-4707")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void checkOldTokenIsInvalidInCaseOfDeleteUser(Role role) {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String newOrganizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String newUserToken = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newUserId = organizationAndUsers.getJSONObject(role.name()).getString("id");
        UserHelper.deleteUser(SUPPORT_TOKEN, newOrganizationId, newUserId)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.searchForUsers(newUserToken, newOrganizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

}
