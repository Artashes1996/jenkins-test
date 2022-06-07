package e2e.gatewayapps.organizationsresource;

import e2e.gatewayapps.BaseTest;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.locationsresource.payloads.LocationStatusChangeRequestBody;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.appsapi.support.organizationsresource.payloads.DeleteRestoreOrganizationRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class RestoreOrganizationTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private ThreadLocal<JSONObject> organizationThread;
    private ThreadLocal<String> organizationIdThread;

    @BeforeClass
    public void init() {
        organizationThread = new ThreadLocal<>();
        organizationIdThread = new ThreadLocal<>();
        organizationFlows = new OrganizationFlows();
    }

    @BeforeMethod(alwaysRun = true)
    public void setup() {
        organizationThread.set(organizationFlows.createAndPublishOrganizationWithOwner());
        organizationIdThread.set(organizationThread.get().getJSONObject("ORGANIZATION").getString("id"));
    }

    @Xray(test = "PEG-1741")
    @Test
    public void restorePausedOrganization() {
        final String organizationStatusBeforeDeletion = organizationFlows.pauseOrganization(organizationIdThread.get()).getString("status");
        organizationFlows.deleteOrganization(organizationIdThread.get());
        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);

        final String organizationStatusAfterDeletion = OrganizationsHelper
                .restoreOrganization(SUPPORT_TOKEN, organizationIdThread.get(), restoreBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json")).extract().path("status");

        assertEquals(organizationStatusBeforeDeletion, organizationStatusAfterDeletion);
    }

    @Xray(test = "PEG-1748")
    @Test
    public void restoreBlockedOrganization() {
        final String locationId = new LocationFlows().createLocation(organizationIdThread.get()).getString("id");
        final String organizationStatusBeforeDeletion = organizationFlows.blockOrganization(organizationIdThread.get()).getString("status");
        organizationFlows.deleteOrganization(organizationIdThread.get());

        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);

        final String organizationStatusAfterDeletion = helpers.appsapi.support.organizationsresource.OrganizationsHelper
                .restoreOrganization(SUPPORT_TOKEN, organizationIdThread.get(), restoreBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"))
                .extract().path("status");
        assertEquals(organizationStatusBeforeDeletion, organizationStatusAfterDeletion);

        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationAndStatus = new JSONObject();
        locationAndStatus.put(locationId, CreateLocationRequestBody.LocationStatuses.INACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationAndStatus);
        LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationIdThread.get(), statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    // TODO check this test
    @Xray(test = "PEG-1749")
    @Test(enabled = false)
    public void restoreLiveOrganization() {
        final JSONObject location = new LocationFlows().createLocation(organizationIdThread.get());
        organizationFlows.deleteOrganization(organizationIdThread.get());

        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);
        final Response body = OrganizationsHelper.restoreOrganization(SUPPORT_TOKEN, organizationIdThread.get(), restoreBody);
        body.then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));

        final JSONObject owner = organizationThread.get().getJSONObject(OWNER.name());
        final Response locationResponseAfterRestore = LocationsHelper.getLocation(SUPPORT_TOKEN, organizationIdThread.get(), location.getString("id"));
        final Response userResponseAfterRestore = UserHelper.getUserById(SUPPORT_TOKEN, organizationIdThread.get(), owner.getString("id"));
        final JSONObject actualOrganization = new JSONObject(body.asString());
        final JSONObject expectedOrganization = organizationThread.get().getJSONObject("ORGANIZATION");
        final JSONObject actualLocation = new JSONObject(locationResponseAfterRestore.asString());
        final JSONObject actualUser = new JSONObject(userResponseAfterRestore.asString());

        assertTrue(actualOrganization.similar(expectedOrganization));
        assertTrue(location.similar(actualLocation));
        assertTrue(owner.keySet().containsAll(actualUser.keySet()));
    }

    @Xray(test = "PEG-1774")
    @Test
    public void restoreNotDeletedOrganization() {
        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);

        OrganizationsHelper.restoreOrganization(SUPPORT_TOKEN, organizationIdThread.get(), restoreBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1779")
    @Test
    public void restoreDeletedOrganizationUnsupportedUser() {
        final UserFlows userFlows = new UserFlows();
        final String locationId = new LocationFlows().createLocation(organizationIdThread.get()).getString("id");

        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);
        final String ownerToken = organizationThread.get().getJSONObject(OWNER.name()).getString("token");
        final String adminToken = userFlows.createUser(organizationIdThread.get(), ADMIN, null).getString("token");
        final String locationAdminToken = userFlows.createUser(organizationIdThread.get(), LOCATION_ADMIN, Collections.singletonList(locationId)).getString("token");
        final String staffToken = userFlows.createUser(organizationIdThread.get(), STAFF, Collections.singletonList(locationId)).getString("token");

        organizationFlows.deleteOrganization(organizationIdThread.get());
        OrganizationsHelper.restoreOrganization(ownerToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.restoreOrganization(adminToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.restoreOrganization(locationAdminToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.restoreOrganization(staffToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1780")
    @Test
    public void restoreOtherOrganization() {
        final UserFlows userFlows = new UserFlows();
        final String locationId = new LocationFlows().createLocation(organizationIdThread.get()).getString("id");

        final String ownerToken = organizationThread.get().getJSONObject(OWNER.name()).getString("token");
        final String adminToken = userFlows.createUser(organizationIdThread.get(), ADMIN, null).getString("token");
        final String locationAdminToken = userFlows.createUser(organizationIdThread.get(), LOCATION_ADMIN, Collections.singletonList(locationId)).getString("token");
        final String staffToken = userFlows.createUser(organizationIdThread.get(), STAFF, Collections.singletonList(locationId)).getString("token");

        final String newOrganizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        organizationFlows.deleteOrganization(newOrganizationId);
        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);

        OrganizationsHelper.restoreOrganization(ownerToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        OrganizationsHelper.restoreOrganization(adminToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        OrganizationsHelper.restoreOrganization(locationAdminToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        OrganizationsHelper.restoreOrganization(staffToken, organizationIdThread, restoreBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    // TODO add XRay
    @Test
    public void restoreUnpublishedOrganization() {

        final String unpublishedOrganizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");

        organizationFlows.deleteOrganization(unpublishedOrganizationId);
        final JSONObject restoreBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);
        OrganizationsHelper.restoreOrganization(SUPPORT_TOKEN, unpublishedOrganizationId, restoreBody)
                .then()
                .statusCode(SC_OK)
                .body("publicationDate", is(nullValue()));
    }

}