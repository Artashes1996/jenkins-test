package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetAccountByIdTest extends BaseTest {

    private JSONObject owner;
    private JSONObject admin;
    private JSONObject locationAdmin;
    private JSONObject staff;
    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;
    private String organizationId;
    private String locationId;

    private String ownerToGetAccountId;
    private String adminToGetAccountId;
    private String locationAdminToGetAccountId;
    private String staffToGetAccountId;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        owner = organizationAndUsers.getJSONObject(OWNER.name());
        admin = organizationAndUsers.getJSONObject(ADMIN.name());
        locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToGetAccountId = DBHelper.getAccountIdByUserId(owner.getString("id"));
        adminToGetAccountId = DBHelper.getAccountIdByUserId(admin.getString("id"));
        locationAdminToGetAccountId = DBHelper.getAccountIdByUserId(locationAdmin.getString("id"));
        staffToGetAccountId = DBHelper.getAccountIdByUserId(staff.getString("id"));

    }

    @Xray(test = "PEG-1468, PEG-1450, PEG-1451")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getAccountById(Role role) {
        final JSONObject user = role.equals(SUPPORT)?null: new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));

        final String token = role.equals(SUPPORT)?SUPPORT_TOKEN: new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));
        AccountHelper.getAccountById(token, organizationId, ownerToGetAccountId)
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(token, organizationId, adminToGetAccountId)
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(token, organizationId, locationAdminToGetAccountId)
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(token, organizationId, staffToGetAccountId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-1452")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void getAccountByIdByInactiveUser(Role role) {
        final JSONObject user = new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));
        userFlows.inactivateUserById(organizationId, user.getString("id"));

        AccountHelper.getAccountById(token, organizationId, ownerToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        AccountHelper.getAccountById(token, organizationId, adminToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        AccountHelper.getAccountById(token, organizationId, locationAdminToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        AccountHelper.getAccountById(token, organizationId, staffToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-1453")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void getAccountByIdByDeletedUser(Role role) {
        final JSONObject user = new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));
        userFlows.deleteUser(organizationId, user.getString("id"));

        AccountHelper.getAccountById(token, organizationId, ownerToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        AccountHelper.getAccountById(token, organizationId, adminToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        AccountHelper.getAccountById(token, organizationId, locationAdminToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        AccountHelper.getAccountById(token, organizationId, staffToGetAccountId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-1453")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void getDeletedAccountById(Role role) {
        final JSONObject user = new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));
        final String userToGetAccountId = DBHelper.getAccountIdByUserId(user.getString("id"));

        AccountHelper.getDeletedAccountById(owner.getString("token"), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_OK);
        AccountHelper.getDeletedAccountById(admin.getString("token"), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_OK);
        AccountHelper.getDeletedAccountById(locationAdmin.getString("token"), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_OK);
        AccountHelper.getDeletedAccountById(staff.getString("token"), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-1467")
    @Test
    public void getOtherOrganizationAccountById() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String userToGetAccountId = organizationAndOwner.getJSONObject(OWNER.name()).getString("id");

        AccountHelper.getAccountById(new AuthenticationFlowHelper().getTokenWithEmail(owner.getString("email")), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_NOT_FOUND);
        AccountHelper.getAccountById(new AuthenticationFlowHelper().getTokenWithEmail(admin.getString("email")), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_NOT_FOUND);
        AccountHelper.getAccountById(new AuthenticationFlowHelper().getTokenWithEmail(locationAdmin.getString("email")), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_NOT_FOUND);
        AccountHelper.getAccountById(new AuthenticationFlowHelper().getTokenWithEmail(staff.getString("email")), organizationId, userToGetAccountId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    // TODO add XRay test case
    @Test
    public void getBlockedOrganizationAccountById() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        organizationFlows.blockOrganization(organizationAndUsers.getJSONObject("ORGANIZATION").getString("id"));
        AccountHelper.getAccountById(owner.getString("token"), organizationId, DBHelper.getAccountIdByUserId(admin.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(admin.getString("token"), organizationId, DBHelper.getAccountIdByUserId(staff.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(locationAdmin.getString("token"), organizationId, DBHelper.getAccountIdByUserId(owner.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(staff.getString("token"), organizationId, DBHelper.getAccountIdByUserId(locationAdmin.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(SUPPORT_TOKEN, organizationId, DBHelper.getAccountIdByUserId(locationAdmin.getString("id")))
                .then()
                .statusCode(SC_OK);
    }


    // TODO add XRay test case
    @Test
    public void getPausedOrganizationAccountById() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String pausedOrganizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        organizationFlows.pauseOrganization(pausedOrganizationId);
        AccountHelper.getAccountById(owner.getString("token"), pausedOrganizationId, DBHelper.getAccountIdByUserId(admin.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(admin.getString("token"), pausedOrganizationId, DBHelper.getAccountIdByUserId(staff.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(locationAdmin.getString("token"), pausedOrganizationId, DBHelper.getAccountIdByUserId(owner.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(staff.getString("token"), pausedOrganizationId, DBHelper.getAccountIdByUserId(locationAdmin.getString("id")))
                .then()
                .statusCode(SC_OK);
        AccountHelper.getAccountById(SUPPORT_TOKEN, pausedOrganizationId, DBHelper.getAccountIdByUserId(locationAdmin.getString("id")))
                .then()
                .statusCode(SC_OK);
    }
}
