package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetCurrentAccountTest extends BaseTest {

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;
    private OrganizationFlows organizationFlows;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        staffToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
    }

    // TODO XRay is missing
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getCurrentAccount(Role role) {
        final String token = role.equals(SUPPORT)?SUPPORT_TOKEN:role.equals(OWNER)?ownerToken:role.equals(ADMIN)?adminToken:role.equals(LOCATION_ADMIN)?locationAdminToken:staffToken;
        AccountHelper.getCurrentAccount(token)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/account.json"));
    }

    // TODO XRay is missing
    @Test
    public void getCurrentAccountWithoutAuth() {
        AccountHelper.getCurrentAccount(null)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // TODO XRay is missing
    @Test
    public void getCurrentAccountPausedOrganization() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndOwner.getJSONObject(OWNER.name()).getString("email"));
        organizationFlows.pauseOrganization(organizationAndOwner.getJSONObject("ORGANIZATION").getString("id"));

        AccountHelper.getCurrentAccount(ownerToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/account.json"));
    }

    // TODO XRay is missing
    @Test
    public void getCurrentAccountBlockedOrganization() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndOwner.getJSONObject(OWNER.name()).getString("email"));
        organizationFlows.blockOrganization(organizationAndOwner.getJSONObject("ORGANIZATION").getString("id"));

        AccountHelper.getCurrentAccount(ownerToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/account.json"));
    }

    // TODO maybe better to move this into deleted organization test case
    @Test
    public void getCurrentAccountDeletedOrganization() {
        final JSONObject organizationAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndOwner.getJSONObject(OWNER.name()).getString("email"));
        organizationFlows.deleteOrganization(organizationAndOwner.getJSONObject("ORGANIZATION").getString("id"));

        AccountHelper.getCurrentAccount(SUPPORT_TOKEN)
                .then()
                .statusCode(HttpStatus.SC_OK);
        AccountHelper.getCurrentAccount(ownerToken)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
