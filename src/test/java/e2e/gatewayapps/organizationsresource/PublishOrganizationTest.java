package e2e.gatewayapps.organizationsresource;

import e2e.gatewayapps.BaseTest;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.UUID;

import static configuration.Role.OWNER;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class PublishOrganizationTest extends BaseTest {

    private final ThreadLocal<JSONObject> organizationThread = new ThreadLocal<>();
    private final ThreadLocal<String> organizationIdThread = new ThreadLocal<>();
    private final ThreadLocal<JSONObject> owner = new ThreadLocal<>();
    private OrganizationFlows organizationFlows;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        organizationThread.set(organizationFlows.createUnpublishedOrganizationWithOwner());
        organizationIdThread.set(organizationThread.get().getJSONObject("ORGANIZATION").getString("id"));
        owner.set(organizationThread.get().getJSONObject(OWNER.name()));

        final String organizationStatus = organizationThread.get().getJSONObject("ORGANIZATION").getString("status");
        assertEquals(organizationStatus, "PAUSED");
    }

    @Xray(test = "PEG-1586, PEG-1589")
    @Test
    public void publishOrganizationWithSupport() {
        OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/publishOrganization.json"));
        OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1587, PEG-1588")
    @Test
    public void publishOrganizationWithUnsupportedUser() {
        OrganizationsHelper.publishOrganization(owner.get().getString("token"), organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1590")
    @Test
    public void publishPausedOrganization() {
        organizationFlows.publishOrganization(organizationIdThread.get());
        organizationFlows.pauseOrganization(organizationIdThread.get());
        OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1591")
    @Test
    public void publishBlockedOrganization() {
        organizationFlows.publishOrganization(organizationIdThread.get());
        organizationFlows.blockOrganization(organizationIdThread.get());

        OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1592")
    @Test
    public void publishNonExistingOrganization() {
        final String nonExistingOrganizationId = UUID.randomUUID().toString();
        OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, nonExistingOrganizationId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    // TODO should be passed after fixing PEG-7245 issue
    @Test
    public void publishDeletedOrganization() {
        organizationFlows.deleteOrganization(organizationIdThread.get());

        OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, organizationIdThread.get())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("PRECONDITION_VIOLATED"));
    }
}
