package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.support.usersresource.UsersHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.SupportFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.UUID;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;

public class SupportUserDeleteTest extends BaseTest {

    private SupportFlows supportFlows;

    @BeforeClass
    public void setUp() {
        supportFlows = new SupportFlows();
    }

    @Test(testName = "PEG-1799, PEG-1801")
    public void deleteSupportUser() {
        final String email = getRandomInt() + "@qless.com";
        final String supportId = supportFlows.createSupport(email).getString("id");

        UsersHelper.deleteSupportUser(SUPPORT_TOKEN, supportId)
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersHelper.deleteSupportUser(SUPPORT_TOKEN, supportId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1800")
    @Test
    public void deleteInvitedSupport() {
        final String email = getRandomInt() + "@qless.com";
        final String supportId = supportFlows.createSupport(email).getString("id");
        UsersHelper.deleteSupportUser(SUPPORT_TOKEN, supportId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(test = "PEG-1802")
    @Test
    public void deleteNonExistingSupport() {
        final String nonExistingSupportId = UUID.randomUUID().toString();
        UsersHelper.deleteSupportUser(SUPPORT_TOKEN, nonExistingSupportId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-1803, PEG-1804", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void deleteSupportByUnsupportedRole(Role role) {
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final String email = getRandomInt() + "@qless.com";
        final String supportId = supportFlows.createSupport(email).getString("id");

        UsersHelper.deleteSupportUser(ownerToken, supportId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }
}
