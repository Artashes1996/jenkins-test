package e2e.gatewayapps.organizationsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.organizationsresource.data.OrganizationsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;

import helpers.appsapi.support.organizationsresource.payloads.CreateOrganizationRequestBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import static configuration.Role.*;
import static helpers.appsapi.support.organizationsresource.payloads.CreateOrganizationRequestBody.*;
import static helpers.appsapi.support.organizationsresource.payloads.CreateOrganizationRequestBody.WEB_SITE;
import static org.apache.http.HttpStatus.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class CreateOrganizationsTest extends BaseTest {
    
    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(owner.getString("email"));
        adminToken = new AuthenticationFlowHelper().getTokenWithEmail(admin.getString("email"));
        locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(locationAdmin.getString("email"));
        staffToken = new AuthenticationFlowHelper().getTokenWithEmail(staff.getString("email"));
    }

    @Xray(test = "PEG-1079, PEG-1077, PEG-1076, PEG-1068")
    @Test(dataProvider = "valid verticals", dataProviderClass = OrganizationsDataProvider.class)
    public void createOrganizationBySupport(String vertical) {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.ALL_FIELDS);

        createOrganizationBody.put(VERTICAL, vertical);
        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_CREATED)
                .body("internalName", equalTo(createOrganizationBody.getString("internalName")))
                .body("vertical", equalTo(createOrganizationBody.getString("vertical")))
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"));
    }

    @Xray(test = "PEG-1070")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void createOrganizationByOtherUsers(Role role) {
        final String SUPPORT_TOKEN = role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.REQUIRED);

        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1078")
    @Test
    public void createOrganizationWithExistingInternalAndDisplayName() {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.ALL_FIELDS);

        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_CREATED)
                .body("internalName", equalTo(createOrganizationBody.getString(INTERNAL_NAME)))
                .body("vertical", equalTo(createOrganizationBody.getString(VERTICAL)))
                .body("websiteUrl", equalTo(createOrganizationBody.getString(WEB_SITE)));

        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));

        createOrganizationBody.put(INTERNAL_NAME, getRandomInt() + " name");
        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createOrganization.json"));
    }

    @Xray(test = "PEG-1072")
    @Test(dataProvider = "organization invalid values", dataProviderClass = OrganizationsDataProvider.class)
    public void createOrganizationWithInvalidInternalName(Object name) {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.REQUIRED);
        createOrganizationBody.put(INTERNAL_NAME, name);
        final String errorType = Boolean.TRUE.equals(name) ? "NOT_READABLE_REQUEST_BODY" : "DATA_INTEGRITY_CONSTRAINT_VIOLATED";
        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is(errorType));

    }

    @Xray(test = "PEG-1073")
    @Test(dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void createOrganizationInvalidPhoneNumber(Object phoneNumber) {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.REQUIRED);
        createOrganizationBody.put(PHONE_NUMBER, phoneNumber);

        final String errorType = Boolean.TRUE.equals(phoneNumber) ? "NOT_READABLE_REQUEST_BODY" : "DATA_INTEGRITY_CONSTRAINT_VIOLATED";

        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is(errorType));
    }

    @Xray(test = "PEG-1074")
    @Test(dataProvider = "organization invalid values", dataProviderClass = OrganizationsDataProvider.class)
    public void createOrganizationInvalidVertical(Object vertical) {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.REQUIRED);
        createOrganizationBody.put(VERTICAL, vertical);
        final String errorType = vertical == null ? "DATA_INTEGRITY_CONSTRAINT_VIOLATED" : "NOT_READABLE_REQUEST_BODY";

        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is(errorType));
    }

    @Xray(test = "PEG-1071")
    @Test
    public void createOrganizationInvalidWebsiteUrl() {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(OrganizationCreateCombination.REQUIRED);

        createOrganizationBody.put(WEB_SITE, "www.sfl.am");
        OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }
}