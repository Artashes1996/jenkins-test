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

import java.util.UUID;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class OrganizationLogoDeleteTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private String organizationId;
    private final UserFlows userFlows = new UserFlows();
    final String filePath = "src/test/resources/files/pics/charmander.png";

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToken = owner.getString("token");
        adminToken = admin.getString("token");
        locationAdminToken = locationAdmin.getString("token");
        staffToken = staff.getString("token");
    }

    @Xray(test = "PEG-1504")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void deleteLogo(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        organizationFlows.uploadLogoWithFilePath(organizationId, filePath);
        OrganizationsHelper.deleteOrganizationImage(token, organizationId)
                .then()
                .statusCode(SC_NO_CONTENT);
        OrganizationsHelper.deleteOrganizationImage(token, organizationId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    // TODO add xray
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "rolesWithLocation")
    public void deleteLogoUnsupportedUsers(Role role) {
        final String token = role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        organizationFlows.uploadLogoWithFilePath(organizationId, filePath);
        OrganizationsHelper.deleteOrganizationImage(token, organizationId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1507")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void deleteLogoInactiveUser(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, null);
        final String userToken = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));
        userFlows.inactivateUserById(organizationId, user.getString("id"));

        OrganizationsHelper.deleteOrganizationImage(userToken, organizationId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1509")
    @Test
    public void deleteLogoDeletedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        organizationFlows.uploadLogo(organizationId);
        organizationFlows.deleteOrganization(organizationId);

        OrganizationsHelper.deleteOrganizationImage(SUPPORT_TOKEN, organizationId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        OrganizationsHelper.deleteOrganizationImage(ownerToken, organizationId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.deleteOrganizationImage(adminToken, organizationId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    // TODO change xray
    @Xray(test = "PEG-1510")
    @Test
    public void deleteLogoBlockedOrganization() {
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String blockedOrganizationId = blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String adminToken = blockedOrganizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        organizationFlows.uploadLogo(blockedOrganizationId);
        organizationFlows.blockOrganization(blockedOrganizationId);
        OrganizationsHelper.deleteOrganizationImage(SUPPORT_TOKEN, blockedOrganizationId)
                .then()
                .statusCode(SC_NO_CONTENT);
        organizationFlows.uploadLogo(blockedOrganizationId);
        OrganizationsHelper.deleteOrganizationImage(adminToken, blockedOrganizationId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    //TODO change xray
    @Xray(test = "PEG-1510")
    @Test
    public void deleteLogoPausedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        organizationFlows.uploadLogo(organizationId);
        organizationFlows.pauseOrganization(organizationId);

        OrganizationsHelper.deleteOrganizationImage(SUPPORT_TOKEN, organizationId)
                .then()
                .statusCode(SC_NO_CONTENT);
        organizationFlows.uploadLogo(organizationId);
        OrganizationsHelper.deleteOrganizationImage(ownerToken, organizationId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(test = "PEG-1512")
    @Test
    public void deleteLogoInvalidToken() {
        final String invalidToken = UUID.randomUUID().toString();
        OrganizationsHelper.deleteOrganizationImage(invalidToken, organizationId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}