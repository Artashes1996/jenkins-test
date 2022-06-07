package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.organizationsresource.OrganizationsHelper;
import helpers.appsapi.organizationsresource.payloads.UpdateOrganizationRequestBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.UUID;

import static configuration.Role.*;
import static helpers.appsapi.organizationsresource.payloads.UpdateOrganizationRequestBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class UpdateOrganizationTest extends BaseTest {


    private JSONObject organizationAndUsers;
    private OrganizationFlows organizationFlows;
    private String organizationId;
    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(owner.getString("email"));
        adminToken = new AuthenticationFlowHelper().getTokenWithEmail(admin.getString("email"));
        locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(locationAdmin.getString("email"));
        staffToken = new AuthenticationFlowHelper().getTokenWithEmail(staff.getString("email"));
    }


    @Xray(test = "PEG-1235, PEG-1183")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void configureExistingOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final JSONObject configureBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(token, organizationId, configureBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(configureBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(configureBody.getString(PHONE_NUMBER)))
                .body("internalName", equalTo(configureBody.getString(INTERNAL_NAME)));
    }

    @Xray(test = "PEG-1220")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void configureExistingOrganizationByUnsupportedUsers(Role role) {
        final String token = role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        OrganizationsHelper.updateOrganization(token, organizationId, UpdateOrganizationRequestBody.bodyBuilder())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    // TODO add XRay
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void configureNonExistingOrganization(Role role) {
        final String token = role.equals(OWNER) ? ownerToken : adminToken;
        OrganizationsHelper.updateOrganization(token, UUID.randomUUID().toString(), UpdateOrganizationRequestBody.bodyBuilder())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1221")
    @Test
    public void configureNonExistingOrganizationSupport() {
        OrganizationsHelper.updateOrganization(SUPPORT_TOKEN, UUID.randomUUID().toString(), UpdateOrganizationRequestBody.bodyBuilder())
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }


    @Xray(test = "PEG-1227")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void configureExistingOrganizationSetNameNull(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final JSONObject updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        updateBody.put(INTERNAL_NAME, JSONObject.NULL);
        OrganizationsHelper.updateOrganization(token, organizationId, updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1228")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void configureExistingOrganizationByOwnerOnlyName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;

        final JSONObject updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        updateBody.remove(PHONE_NUMBER);
        updateBody.remove(WEBSITE_URL);
        OrganizationsHelper.updateOrganization(token, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(null))
                .body("contactInfo.phoneNumber", equalTo(null))
                .body("internalName", equalTo(updateBody.getString(INTERNAL_NAME)));

    }

    @Xray(test = "PEG-1231")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void configureExistingOrganizationBadFormattedContact(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;

        final String badFormattedContact = "12345678";

        final JSONObject configureBody = UpdateOrganizationRequestBody.bodyBuilder();
        configureBody.put(PHONE_NUMBER, badFormattedContact);
        OrganizationsHelper.updateOrganization(token, organizationId, configureBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    // TODO Add XRay
    @Test
    public void configurePausedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(pausedOrganization.getJSONObject(OWNER.name()).getString("email"));
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(pausedOrganization.getJSONObject(ADMIN.name()).getString("email"));
        organizationFlows.pauseOrganization(organizationId);
        JSONObject updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(SUPPORT_TOKEN, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(updateBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(updateBody.getString(PHONE_NUMBER)))
                .body("internalName", equalTo(updateBody.getString(INTERNAL_NAME)));
        updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(ownerToken, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(updateBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(updateBody.getString(PHONE_NUMBER)));
        updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(adminToken, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(updateBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(updateBody.getString(PHONE_NUMBER)));
    }

    // TODO Add XRay
    @Test
    public void configureBlockedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(pausedOrganization.getJSONObject(OWNER.name()).getString("email"));
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(pausedOrganization.getJSONObject(ADMIN.name()).getString("email"));
        organizationFlows.blockOrganization(organizationId);
        JSONObject updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(SUPPORT_TOKEN, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(updateBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(updateBody.getString(PHONE_NUMBER)))
                .body("internalName", equalTo(updateBody.getString(INTERNAL_NAME)));
        updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(ownerToken, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(updateBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(updateBody.getString(PHONE_NUMBER)))
                .body("internalName", equalTo(updateBody.getString(INTERNAL_NAME)));
        updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(adminToken, organizationId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"))
                .body("websiteUrl", equalTo(updateBody.getString(WEBSITE_URL)))
                .body("contactInfo.phoneNumber", equalTo(updateBody.getString(PHONE_NUMBER)))
                .body("internalName", equalTo(updateBody.getString(INTERNAL_NAME)));
    }

    // TODO Add XRay
    @Test
    public void configureDeletedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(pausedOrganization.getJSONObject(OWNER.name()).getString("email"));
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(pausedOrganization.getJSONObject(ADMIN.name()).getString("email"));
        organizationFlows.deleteOrganization(organizationId);
        JSONObject updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        OrganizationsHelper.updateOrganization(SUPPORT_TOKEN, organizationId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        OrganizationsHelper.updateOrganization(ownerToken, organizationId, updateBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        OrganizationsHelper.updateOrganization(adminToken, organizationId, updateBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-7422", requirement = "PEG-7115")
    @Test
    public void checkOrganizationDuplication() {
        final String organizationInternalName = organizationFlows.createUnpublishedOrganization().getString("internalName");
        final Role randomAdminRole = getRandomAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final JSONObject updateBody = UpdateOrganizationRequestBody.bodyBuilder();
        updateBody.put(INTERNAL_NAME, organizationInternalName);
        OrganizationsHelper.updateOrganization(token, organizationId, updateBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

}
