package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.organizationsresource.data.OrganizationsDataProvider;
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
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class OrganizationLogoUploadTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private String organizationId;
    private UserFlows userFlows;
    final String filePath = "src/test/resources/files/pics/charmander.png";

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
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

    @Xray(test = "PEG-1118")
    @Test(dataProvider = "valid logo paths", dataProviderClass = OrganizationsDataProvider.class)
    public void uploadLogoSupport(String filePath) {
        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));
    }

    @Xray(test = "PEG-1494")
    @Test(dataProvider = "valid logo paths", dataProviderClass = OrganizationsDataProvider.class)
    public void uploadLogoOwner(String filePath) {
        OrganizationsHelper.uploadOrganizationImage(ownerToken, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));
    }

    // TODO add xray
    @Test(dataProvider = "valid logo paths", dataProviderClass = OrganizationsDataProvider.class)
    public void uploadLogoAdmin(String filePath) {
        OrganizationsHelper.uploadOrganizationImage(adminToken, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));
    }

    // TODO add xray
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "rolesWithLocation")
    public void uploadLogoUnsupportedUsers(Role role) {
        final String token = role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        OrganizationsHelper.uploadOrganizationImage(token, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    // TODO change xray
    @Xray(test = "PEG-1119")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void uploadLogoWrongContentType(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        OrganizationsHelper.uploadOrganizationImage(token, organizationId, filePath, "text/html")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1120")
    @Test
    public void uploadLogoSupportNonExistingOrganization() {
        final String organizationId = UUID.randomUUID().toString();
        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1130")
    @Test(dataProvider = "unsupported files", dataProviderClass = OrganizationsDataProvider.class)
    public void uploadUnsupportedFileSupport(String unsupportedFilePath) {
        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, unsupportedFilePath, "image/png")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1497")
    @Test(dataProvider = "unsupported files", dataProviderClass = OrganizationsDataProvider.class)
    public void uploadUnsupportedFileOwner(String unsupportedFilePath) {
        OrganizationsHelper.uploadOrganizationImage(ownerToken, organizationId, unsupportedFilePath, "image/png")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1497")
    @Test(dataProvider = "unsupported files", dataProviderClass = OrganizationsDataProvider.class)
    public void uploadUnsupportedFileAdmin(String unsupportedFilePath) {
        OrganizationsHelper.uploadOrganizationImage(adminToken, organizationId, unsupportedFilePath, "image/png")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1131")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void uploadUnsupportedFileWithCorrespondingContentTypeSupport(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final String unsupportedFilePath = "src/test/resources/files/Captain Marvel.html";
        OrganizationsHelper.uploadOrganizationImage(token, organizationId, unsupportedFilePath, "text/html")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1132")
    @Test
    public void uploadLogoNoOrganizationIdSupport() {
        final String organizationId = "";
        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("error", is("Not Found"));
    }

//  TODO should be work after fixing PEG-7229 issue
    @Xray(test = "PEG-1133")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void uploadUnsupportedSizeFile(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final String unsupportedSizeFilePath = "src/test/resources/files/pics/bubble.jpg";
        OrganizationsHelper.uploadOrganizationImage(token, organizationId, unsupportedSizeFilePath, "image/png")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1496")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void uploadLogoInactiveUser(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, null);
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));
        final String userId = user.getString("id");
        userFlows.inactivateUserById(organizationId, userId);

        OrganizationsHelper.uploadOrganizationImage(token, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1500")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void uploadLogoDeletedUser(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, null);
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));
        final String userId = user.getString("id");
        userFlows.deleteUser(organizationId, userId);

        OrganizationsHelper.uploadOrganizationImage(token, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-1534")
    @Test
    public void uploadLogoOfPausedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(ADMIN.name()).getString("email"));
        organizationFlows.pauseOrganization(organizationId);

        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));

        OrganizationsHelper.uploadOrganizationImage(adminToken, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));
    }

    @Xray(test = "PEG-1534")
    @Test
    public void uploadLogoOfBlockedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(OWNER.name()).getString("email"));
        organizationFlows.blockOrganization(organizationId);

        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));

        OrganizationsHelper.uploadOrganizationImage(ownerToken, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/uploadOrganizationLogo.json"));
    }

    @Xray(test = "PEG-1503")
    @Test
    public void uploadLogoOfDeletedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        organizationFlows.deleteOrganization(organizationId);

        OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));

        OrganizationsHelper.uploadOrganizationImage(ownerToken, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        OrganizationsHelper.uploadOrganizationImage(adminToken, organizationId, filePath, "image/png")
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}
