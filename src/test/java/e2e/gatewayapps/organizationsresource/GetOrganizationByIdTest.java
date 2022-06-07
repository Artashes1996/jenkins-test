package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.organizationsresource.OrganizationsHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Random;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetOrganizationByIdTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private String organizationId;
    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    private String organizationIdToPause;
    private String ownerTokenOfPausedOrganization;
    private String adminTokenOfPausedOrganization;
    private String locationAdminTokenOfPausedOrganization;
    private String staffTokenOfPausedOrganization;

    private String organizationIdToBlock;
    private String ownerTokenOfBlockedOrganization;
    private String adminTokenOfBlockedOrganization;
    private String locationAdminTokenOfBlockedOrganization;
    private String staffTokenOfBlockedOrganization;

    private String organizationIdToDelete;
    private String ownerTokenOfDeletedOrganization;
    private String adminTokenOfDeletedOrganization;
    private String locationAdminTokenOfDeletedOrganization;
    private String staffTokenOfDeletedOrganization;


    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        staffToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationIdToPause = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerTokenOfPausedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        adminTokenOfPausedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        locationAdminTokenOfPausedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        staffTokenOfPausedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        organizationFlows.pauseOrganization(organizationIdToPause);

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationIdToBlock = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerTokenOfBlockedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        adminTokenOfBlockedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        locationAdminTokenOfBlockedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        staffTokenOfBlockedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        organizationFlows.blockOrganization(organizationIdToBlock);

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationIdToDelete = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerTokenOfDeletedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        adminTokenOfDeletedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        locationAdminTokenOfDeletedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        staffTokenOfDeletedOrganization = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(STAFF.name()).getString("email"));
        organizationFlows.deleteOrganization(organizationIdToDelete);
    }

    @Xray(test = "PEG-1210, PEG-1211")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        OrganizationsHelper.getOrganizationById(token, organizationId, false)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));
        OrganizationsHelper.getOrganizationById(token, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));
    }

    @Xray(test = "PEG-1241")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getPausedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ?
                ownerTokenOfPausedOrganization : role.equals(ADMIN) ? adminTokenOfPausedOrganization : role.equals(LOCATION_ADMIN) ?
                locationAdminTokenOfPausedOrganization : staffTokenOfPausedOrganization;

        OrganizationsHelper.getOrganizationById(token, organizationIdToPause, true)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));

        OrganizationsHelper.getOrganizationById(token, organizationIdToPause, false)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));
    }

    @Xray(test = "PEG-1240")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getBlockedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ?
                ownerTokenOfBlockedOrganization : role.equals(ADMIN) ? adminTokenOfBlockedOrganization : role.equals(LOCATION_ADMIN) ?
                locationAdminTokenOfBlockedOrganization : staffTokenOfBlockedOrganization;

        OrganizationsHelper.getOrganizationById(token, organizationIdToBlock, true)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));

        OrganizationsHelper.getOrganizationById(token, organizationIdToBlock, false)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/getOrganizationById.json"));
    }

    @Xray(test = "PEG-1242")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void getDeletedOrganizationByNotSupportedUsers(Role role) {
        final String token = role.equals(OWNER) ?
                ownerTokenOfDeletedOrganization : role.equals(ADMIN) ? adminTokenOfDeletedOrganization : role.equals(LOCATION_ADMIN) ?
                locationAdminTokenOfDeletedOrganization : staffTokenOfDeletedOrganization;

        OrganizationsHelper.getOrganizationById(token, organizationIdToDelete, true)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        OrganizationsHelper.getOrganizationById(token, organizationIdToDelete, false)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1243")
    @Test
    public void getDeletedOrganizationBySupport() {
        OrganizationsHelper.getOrganizationById(SUPPORT_TOKEN, organizationIdToDelete, true)
                .then()
                .statusCode(SC_OK);

        OrganizationsHelper.getOrganizationById(SUPPORT_TOKEN, organizationIdToDelete, false)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1216")
    @Test
    public void getOrganizationByInvalidIdSupport() {
        final String organizationId = new Random().nextInt() + "";
        OrganizationsHelper.getOrganizationById(SUPPORT_TOKEN, organizationId, false)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1219")
    @Test
    public void getOrganizationByIdWithOwnerOfOtherOrganization() {
        final JSONObject organizationWithOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String email = organizationWithOwner.getJSONObject(OWNER.name()).getString("email");
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(email);

        OrganizationsHelper.getOrganizationById(ownerToken, organizationId, false)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1244")
    @Test
    public void getOrganizationByIdInactiveOwner() {
        final UserFlows userFlows = new UserFlows();
        final JSONObject owner = userFlows.createUser(organizationId, OWNER, null);

        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(owner.getString("email"));
        userFlows.inactivateUserById(organizationId, owner.getString("id"));

        OrganizationsHelper.getOrganizationById(ownerToken, organizationId, true)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1239")
    @Test
    public void getOrganizationByIdInvalidToken() {
        OrganizationsHelper.getOrganizationById(null, organizationId, true)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}
