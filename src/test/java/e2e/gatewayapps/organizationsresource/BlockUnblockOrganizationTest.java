package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.organizationsresource.data.OrganizationsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.appsapi.support.organizationsresource.payloads.BlockUnblockOrganizationRequestBody;
import helpers.flows.UserFlows;
import lombok.SneakyThrows;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static org.apache.http.HttpStatus.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static helpers.appsapi.support.organizationsresource.payloads.BlockUnblockOrganizationRequestBody.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class BlockUnblockOrganizationTest extends BaseTest {

    private ThreadLocal<String> organizationThread;
    private String comment;

    @BeforeClass
    public void setup(){
        organizationThread = new ThreadLocal<>();
        comment = UUID.randomUUID().toString();
    }

    @SneakyThrows
    @BeforeMethod(alwaysRun = true)
    public void init() {
        final JSONObject organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        organizationThread.set(organizationWithUsers.getJSONObject("ORGANIZATION").getString("id"));
    }

    @Xray(test = "PEG-1601, PEG-1606, PEG-1609")
    @Test(dataProvider = "blockReasons", dataProviderClass = OrganizationsDataProvider.class)
    public void blockUnblockOrganization(String reason) {
        final JSONObject blockRequestBody = new JSONObject();
        final JSONArray blockReason = new JSONArray();
        blockReason.put(reason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, blockReason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationThread.get(), blockRequestBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/blockUnblockOrganization.json"));

        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationThread.get(), blockRequestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));

        final JSONObject unblockRequestBody = new JSONObject();
        final JSONArray unblockReasons = new JSONArray();
        unblockReasons.put(UnblockReasons.CONTRACT_SIGNED);
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unblockReasons);
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);

        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationThread.get(), unblockRequestBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/blockUnblockOrganization.json"));

    }

    @Xray(test = "PEG-1602")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void blockOrganizationUnsupportedRoles(Role role) {
        final String locationId = new LocationFlows().createLocation(organizationThread.get()).getString("id");
        final String email = new UserFlows().createUser(organizationThread.get(), role, Collections.singletonList(locationId)).getString("email");
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(email);
        new OrganizationFlows().blockOrganization(organizationThread.get());

        final JSONObject blockRequestBody = new JSONObject();
        final JSONArray blockReason = new JSONArray();
        final String reason = BlockReasons.LACK_OF_PAYMENT.name();
        blockReason.put(reason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, blockReason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(token, organizationThread.get(), blockRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1610, PEG-1611")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void unblockOrganizationWithUnsupportedRoles(Role role) {
        final String locationId = new LocationFlows().createLocation(organizationThread.get()).getString("id");
        final String email = new UserFlows().createUser(organizationThread.get(), role, Collections.singletonList(locationId)).getString("email");
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(email);
        new OrganizationFlows().blockOrganization(organizationThread.get());

        final JSONObject unBlockRequestBody = new JSONObject();
        final JSONArray unBlockReason = new JSONArray();
        unBlockReason.put(UnblockReasons.CONTRACT_SIGNED.name());
        unBlockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unBlockReason);
        unBlockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(token, organizationThread.get(), unBlockRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1604")
    @Test
    public void blockUnblockPausedOrganization() {
        new OrganizationFlows().pauseOrganization(organizationThread.get());
        final JSONObject blockRequestBody = new JSONObject();
        final JSONArray blockReason = new JSONArray();
        final String comment = UUID.randomUUID().toString();
        final String reason = BlockReasons.OTHER.name();
        blockReason.put(reason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, blockReason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationThread.get(), blockRequestBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/blockUnblockOrganization.json"));

        final JSONObject unblockRequestBody = new JSONObject();
        final JSONArray unblockReasons = new JSONArray();
        unblockReasons.put(UnblockReasons.CONTRACT_SIGNED);
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unblockReasons);
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);

        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationThread.get(), unblockRequestBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/blockUnblockOrganization.json"));
    }

    @Xray(test = "PEG-1605")
    @Test
    public void blockUnpublishedOrganization() {
        final String organizationId = new OrganizationFlows().createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject blockRequestBody = new JSONObject();
        final JSONArray blockReason = new JSONArray();
        final String comment = UUID.randomUUID().toString();
        final String reason = BlockUnblockOrganizationRequestBody.BlockReasons.OTHER.name();
        blockReason.put(reason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, blockReason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationId, blockRequestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1607")
    @Test
    public void blockNonExistingOrganization() {
        final String organizationId= UUID.randomUUID().toString();
        final JSONObject blockRequestBody = new JSONObject();
        final JSONArray blockReason = new JSONArray();
        final String comment = UUID.randomUUID().toString();
        final String reason = BlockReasons.OTHER.name();
        blockReason.put(reason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, blockReason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationId, blockRequestBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1608")
    @Test
    public void blockDeletedOrganization() {
        new OrganizationFlows().deleteOrganization(organizationThread.get());
        final JSONObject blockRequestBody = new JSONObject();
        final JSONArray blockReason = new JSONArray();
        final String comment = UUID.randomUUID().toString();
        final String reason = BlockReasons.OTHER.name();
        blockReason.put(reason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, blockReason);
        blockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationThread.get(), blockRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1612")
    @Test
    public void unblockPausedOrganization() {
        new OrganizationFlows().pauseOrganization(organizationThread.get());
        final JSONObject unblockRequestBody = new JSONObject();
        final JSONArray unblockReason = new JSONArray();
        final String comment = UUID.randomUUID().toString();
        unblockReason.put(UnblockReasons.CONTRACT_SIGNED.name());
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unblockReason);
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationThread.get(), unblockRequestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1613")
    @Test
    public void unblockLiveOrganization() {
        final JSONObject unblockRequestBody = new JSONObject();
        final JSONArray unblockReason = new JSONArray();
        final String comment = UUID.randomUUID().toString();
        unblockReason.put(UnblockReasons.CONTRACT_SIGNED.name());
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unblockReason);
        unblockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationThread.get(), unblockRequestBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-1614")
    @Test
    public void unblockedBlockedAndDeletedOrganization() {
        new OrganizationFlows().blockOrganization(organizationThread.get());
        new OrganizationFlows().deleteOrganization(organizationThread.get());

        final JSONObject unBlockRequestBody = new JSONObject();
        final JSONArray unBlockReason = new JSONArray();
        unBlockReason.put(UnblockReasons.CONTRACT_SIGNED.name());
        unBlockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unBlockReason);
        unBlockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationThread.get(), unBlockRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1615")
    @Test
    public void unblockNonExistingOrganization() {
        final String comment = UUID.randomUUID().toString();
        final String organizationId = UUID.randomUUID().toString();
        final JSONObject unBlockRequestBody = new JSONObject();
        final JSONArray unBlockReason = new JSONArray();
        unBlockReason.put(UnblockReasons.CONTRACT_SIGNED.name());
        unBlockRequestBody.put(BlockUnblockOrganizationRequestBody.REASONS, unBlockReason);
        unBlockRequestBody.put(BlockUnblockOrganizationRequestBody.COMMENT, comment);
        helpers.appsapi.support.organizationsresource.OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationId, unBlockRequestBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));

    }

}
