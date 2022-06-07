package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.accountresource.data.PasswordDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.accountresource.payloads.ChangePasswordRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.AccountFlows;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static configuration.Role.getRandomOrganizationRole;
import static helpers.appsapi.accountresource.payloads.ChangePasswordRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;

public class ChangePasswordTest extends BaseTest {

    private String organizationId;
    private String locationId;
    private UserFlows userFlows;

    @BeforeClass
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = new LocationFlows().createLocation(organizationId).getString("id");
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4664")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void changePassword(Role role) {
        final String userToken = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4665")
    @Test(dataProviderClass = PasswordDataProvider.class, dataProvider = "invalidPasswords")
    public void changePasswordIncorrectNewPassword(Object password) {
        final String userToken = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        requestBody.put(NEW_PASSWORD, password);
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4666")
    @Test
    public void changePasswordInvalidNewPassword() {
        final String userToken = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        requestBody.put(NEW_PASSWORD, true);
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4667")
    @Test
    public void changePasswordEmptyCurrentPassword() {
        final String userToken = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        requestBody.put(CURRENT_PASSWORD, JSONObject.NULL);
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4668")
    @Test
    public void changePasswordIncorrectCurrentPassword() {
        final String userToken = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        requestBody.put(CURRENT_PASSWORD, "Obj123!");
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("types", hasItem("CURRENT_PASSWORD_MISMATCH"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4669")
    @Test
    public void changePasswordEmptyBody() {
        final String userToken = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = new JSONObject();
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types", hasItem("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4670")
    @Test
    public void changePasswordSameCurrentAndNew() {
        final String userToken = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        requestBody.put(NEW_PASSWORD, requestBody.getString(CURRENT_PASSWORD));
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("types", hasItem("OLD_PASSWORD_PROVIDED"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4671")
    @Test
    public void changePasswordInvalidToken() {
        final String userToken = UUID.randomUUID().toString();
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        AccountHelper.changePassword(userToken, requestBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4763")
    @Test
    public void changePasswordByDeletedUser() {
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        userFlows.deleteUser(organizationId, user.getString("id"));
        AccountHelper.changePassword(user.getString("token"), requestBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types", hasItem("UNAUTHORIZED_ACCESS"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4764")
    @Test
    public void changePasswordByInactiveUser() {
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        userFlows.inactivateUserById(organizationId, user.getString("id"));
        AccountHelper.changePassword(user.getString("token"), requestBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types", hasItem("UNAUTHORIZED_ACCESS"));
    }

    @Xray(requirement = "PEG-3376", test = "PEG-4765")
    @Test
    public void changePasswordUserAfterForceResettingPassword() {
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        new AccountFlows().forceResetUserById(organizationId, user.getString("id"));

        AccountHelper.changePassword(user.getString("token"), requestBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types", hasItem("UNAUTHORIZED_ACCESS"));
    }


    @Xray(requirement = "4707", test = "PEG-5249")
    @Test
    public void oldTokenIsInvalidInCaseOfChangePassword() {
        final JSONObject userObject = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String userId = userObject.getString("id");
        final String token = userObject.getString("token");
        final JSONObject requestBody = ChangePasswordRequestBody.bodyBuilder();
        AccountHelper.changePassword(token, requestBody)
                .then()
                .statusCode(SC_NO_CONTENT);
        UserHelper.getUserById(token, organizationId, userId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", is("UNAUTHORIZED_ACCESS"));
    }
}
