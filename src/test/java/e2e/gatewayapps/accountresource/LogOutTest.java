package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.accountresource.LoginHelper;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.SupportFlows;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.TestUtils;
import utils.Xray;

import java.util.UUID;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;


public class LogOutTest extends BaseTest {

    private JSONObject organizationAndUsersObject;
    private static String organizationId;

    @BeforeClass
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");

    }

    @Xray(test = "PEG-4853", requirement = "PEG-4535")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void logOutWithAllOrganizationRoles(Role role) {
        final String userToken = organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        LoginHelper.logout(userToken)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(userToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", equalTo("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-7022", requirement = "PEG-4535")
    @Test
    public void logOutWithSupport() {
        final JSONObject support = new SupportFlows().createSupport(TestUtils.getRandomInt() + "@qless.com");

        final String userToken = support.getString("token");
        LoginHelper.logout(userToken)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(userToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", equalTo("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-4854", requirement = "PEG-4535")
    @Test
    public void checkIfFirstTimeGeneratedTokenIsValidByRandomRole() {
        final JSONObject randomUserJSONOBJECT = organizationAndUsersObject.getJSONObject(Role.getRandomOrganizationInviterRole().name());
        final String firstUserToken = randomUserJSONOBJECT.getString("token");
        final String userEmail = randomUserJSONOBJECT.getString("email");
        final String secondUserToken = new AuthenticationFlowHelper().getTokenWithEmail(userEmail);
        LoginHelper.logout(secondUserToken)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(firstUserToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-4855", requirement = "PEG-4535")
    @Test
    public void logOutWithInvalidToken() {
        final String invalidToken = UUID.randomUUID().toString();
        LoginHelper.logout(invalidToken)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", equalTo("UNAUTHORIZED_ACCESS"))
                .body("messages[0]", equalTo("Failed to authorize the user"));
    }

    @Xray(test = "PEG-4856", requirement = "PEG-4535")
    @Test
    public void logOutWithoutToken() {
        final String invalidToken = UUID.randomUUID().toString();
        LoginHelper.logout(invalidToken)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", equalTo("UNAUTHORIZED_ACCESS"))
                .body("messages[0]", equalTo("Failed to authorize the user"));
    }

}
