package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.organizationsresource.OrganizationsHelper;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.UUID;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class PauseUnpauseOrganizationTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private ThreadLocal<JSONObject> organizationThread;
    private ThreadLocal<String> organizationIdThread;
    private UserFlows userFlows;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        organizationThread = new ThreadLocal<>();
        organizationIdThread = new ThreadLocal<>();
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
    }

    @BeforeMethod(alwaysRun = true)
    public void init() {
        organizationThread.set(organizationFlows.createAndPublishOrganizationWithAllUsers());
        organizationIdThread.set(organizationThread.get().getJSONObject("ORGANIZATION").getString("id"));
    }

    @Xray(test = "PEG-1593, PEG-1594, PEG-1597, PEG-1642, PEG-1643")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void pauseOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationThread.get().getJSONObject(role.name()).getString("token");
        OrganizationsHelper.pauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/pauseUnpauseOrganization.json"));

        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/pauseUnpauseOrganization.json"));
    }

    // TODO add XRay
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void pauseOrganizationUnsupportedUsers(Role role) {
        final String token = organizationThread.get().getJSONObject(role.name()).getString("token");
        OrganizationsHelper.pauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));

        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1598, PEG-1645")
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void pauseUnpauseBlockedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationThread.get().getJSONObject(role.name()).getString("token");
        organizationFlows.blockOrganization(organizationIdThread.get());

        OrganizationsHelper.pauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1646")
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void unpauseLiveOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationThread.get().getJSONObject(role.name()).getString("token");
        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1599, PEG-1648")
    @Test
    public void pauseUnpauseUnpublishedOrganization() {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        OrganizationsHelper.pauseOrganization(SUPPORT_TOKEN, organizationId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
        OrganizationsHelper.unpauseOrganization(SUPPORT_TOKEN, organizationId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1596")
    @Test
    public void pauseDeletedOrganization() {
        organizationFlows.deleteOrganization(organizationIdThread.get());

        OrganizationsHelper.pauseOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        final String ownerToken = organizationThread.get().getJSONObject(OWNER.name()).getString("token");
        OrganizationsHelper.pauseOrganization(ownerToken, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        final String adminToken = organizationThread.get().getJSONObject(ADMIN.name()).getString("token");
        OrganizationsHelper.pauseOrganization(adminToken, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1647")
    @Test
    public void unpauseDeletedOrganization() {
        organizationFlows.pauseOrganization(organizationIdThread.get());
        organizationFlows.deleteOrganization(organizationIdThread.get());
        OrganizationsHelper.unpauseOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        final String ownerToken = organizationThread.get().getJSONObject(OWNER.name()).getString("token");
        OrganizationsHelper.unpauseOrganization(ownerToken, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        final String adminToken = organizationThread.get().getJSONObject(ADMIN.name()).getString("token");
        OrganizationsHelper.unpauseOrganization(adminToken, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1600")
    @Test
    public void pauseNonExistingOrganization() {
        final String nonExistingOrganizationId = UUID.randomUUID().toString();
        OrganizationsHelper.pauseOrganization(SUPPORT_TOKEN, nonExistingOrganizationId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1649")
    @Test
    public void unpauseOrganizationWithOtherOrganizationOwner() {
        final String otherOrganizationOwnerEmail = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject(OWNER.name()).getString("email");
        final String otherOrganizationOwnerToken = new AuthenticationFlowHelper().getTokenWithEmail(otherOrganizationOwnerEmail);
        OrganizationsHelper.unpauseOrganization(otherOrganizationOwnerToken, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));

        organizationFlows.unpauseOrganization(organizationIdThread.get());
        OrganizationsHelper.pauseOrganization(otherOrganizationOwnerToken, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    // TODO add XRay
    @Test
    public void pauseUnpauseByDeletedUser() {
        final JSONObject owner = userFlows.createUser(organizationIdThread.get(), OWNER, null);
        final String token = owner.getString("token");
        userFlows.deleteUser(organizationIdThread.get(), owner.getString("id"));
        OrganizationsHelper.pauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        organizationFlows.pauseOrganization(organizationIdThread.get());
        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    // TODO add XRay
    @Test
    public void pauseUnpauseByInactiveUser() {
        final JSONObject owner = userFlows.createUser(organizationIdThread.get(), OWNER, null);
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(owner.getString("email"));
        userFlows.inactivateUserById(organizationIdThread.get(), owner.getString("id"));
        OrganizationsHelper.pauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        organizationFlows.pauseOrganization(organizationIdThread.get());
        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    // TODO add XRay
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void pausePausedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationThread.get().getJSONObject(role.name()).getString("token");
        organizationFlows.pauseOrganization(organizationIdThread.get());
        OrganizationsHelper.pauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    // TODO add XRay
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void unpauseUnpausedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationThread.get().getJSONObject(role.name()).getString("token");
        OrganizationsHelper.unpauseOrganization(token, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }
}
