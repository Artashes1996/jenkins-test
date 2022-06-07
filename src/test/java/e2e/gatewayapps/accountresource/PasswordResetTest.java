package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.accountresource.data.AccountDetailsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.ResetPasswordApplyBody;
import helpers.appsapi.accountresource.payloads.ResetPasswordRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.AccountFlows;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;
import java.util.Objects;

import static configuration.Role.getRandomOrganizationRole;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class PasswordResetTest extends BaseTest {

    private UserFlows userFlows;
    private String organizationId;
    private String locationId;

    @BeforeClass
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = new LocationFlows().createLocation(organizationId).getString("id");

    }

    @Test(testName = "PEG-687, PEG-685", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void requestPasswordResetDeletedAccount(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject resetPasswordBody = new JSONObject();
        resetPasswordBody.put(ResetPasswordRequestBody.EMAIL, user.getString("email"));
        AccountHelper.resetPasswordRequest(resetPasswordBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @SneakyThrows
    @Test(testName = "PEG-686, PEG-689, PEG-693", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void requestPasswordResetInactiveAccount(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.inactivateUserById(organizationId, user.getString("id"));
        Thread.sleep(500);
        final JSONObject resetRequestBody = new JSONObject();
        resetRequestBody.put(ResetPasswordRequestBody.EMAIL, user.getString("email"));
        AccountHelper.resetPasswordRequest(resetRequestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("messages[0]", equalTo("Account is not Active"));
    }

    @Test(testName = "PEG-727", dataProvider = "invalidEmail", dataProviderClass = AccountDetailsDataProvider.class)
    public void requestPasswordResetInvalidEmail(Object email) {
        final JSONObject resetPasswordRequestBody = new JSONObject();
        resetPasswordRequestBody.put(ResetPasswordRequestBody.EMAIL, email);
        AccountHelper.resetPasswordRequest(resetPasswordRequestBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test(testName = "PEG-675, PEG-767, PEG-682, PEG-768, PEG-683, PEG-753", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void setNewPassword(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));

        final String validPassword = "Qw12345678!";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, validPassword);

        new AccountFlows().resetPasswordRequest(user.getString("email"));
        String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetToken);
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(user.getString("email")));
    }

    @Test(testName = "PEG-760", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void resetInvalidPassword(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));

        final String invalidPassword = "Q123456";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, invalidPassword);

        new AccountFlows().resetPasswordRequest(user.getString("email"));
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetToken);
        AccountHelper.resetPasswordRequest(applyResetPasswordBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test(testName = "PEG-761")
    public void resetInvalidToken() {
        final String resetInvalidToken = "123";
        final String validPassword = "AwA987676434!&";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, validPassword);
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetInvalidToken);
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-762", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void resetPasswordExpiredToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));

        final String valid = "Qw123456!";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, valid);

        new AccountFlows().resetPasswordRequest(user.getString("email"));
        DBHelper.expireResetPasswordToken(user.getString("id"));
        String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetToken);
        AccountHelper.resetPasswordRequest(applyResetPasswordBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test(testName = "PEG-763", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void reuseResetPasswordToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));

        final String validPassword = "Qw123456!";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, validPassword);

        new AccountFlows().resetPasswordRequest(user.getString("email"));
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetToken);
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(user.getString("email")));
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_GONE);
    }

    @Test(testName = "PEG-762", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void setNewPasswordExpiredToken(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));

        final String validPassword = "WEr123456*";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, validPassword);

        new AccountFlows().resetPasswordRequest(user.getString("email"));
        DBHelper.expireResetPasswordToken(user.getString("id"));
        final String expiredToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, expiredToken);
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_GONE);
    }

    @Test(testName = "PEG-690, PEG-694", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void resetPasswordAccountPendingState(Role role) {
        final JSONObject user = userFlows.inviteUser(organizationId, role, Collections.singletonList(locationId));

        final JSONObject resetPasswordBody = new JSONObject();
        resetPasswordBody.put(ResetPasswordRequestBody.EMAIL, user.getString("email"));
        AccountHelper.resetPasswordRequest(resetPasswordBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-691, PEG-700", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void resetPasswordAccountExpiredState(Role role) {
        final JSONObject user = userFlows.inviteUser(organizationId, role, Collections.singletonList(locationId));
        DBHelper.expireInvitationToken(DBHelper.getInvitationToken(user.getString("email")));

        final JSONObject resetPasswordBody = new JSONObject();
        resetPasswordBody.put(ResetPasswordRequestBody.EMAIL, user.getString("email"));

        AccountHelper.resetPasswordRequest(resetPasswordBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(requirement = "4707", test = "PEG-5250")
    @Test
    public void oldTokenIsInvalidInCaseOfResetPassword() {
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String userToken = user.getString("token");
        final String userId = user.getString("id");
        final String validPassword = "Qw12345678!";
        final JSONObject applyResetPasswordBody = new JSONObject();
        applyResetPasswordBody.put(ResetPasswordApplyBody.PASSWORD, validPassword);

        new AccountFlows().resetPasswordRequest(user.getString("email"));
        final String resetToken = Objects.requireNonNull(DBHelper.getResetPasswordToken(user.getString("id"))).toString();
        applyResetPasswordBody.put(ResetPasswordApplyBody.RESET_TOKEN, resetToken);
        AccountHelper.applyResetPassword(applyResetPasswordBody)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(user.getString("email")));
        UserHelper.getUserFullDetailsById(userToken, organizationId, userId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", is("UNAUTHORIZED_ACCESS"));
    }

}
