package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.serviceresource.data.ServiceDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Random;

import static org.apache.http.HttpStatus.*;
import static configuration.Role.*;
import static helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class ServiceCreationTest extends BaseTest {

    private String organizationId;
    private final String password = "Qw!123456";
    private String ownerToken;
    private String adminToken;
    private UserFlows userFlows;

    @BeforeClass
    public void init() {
        userFlows = new UserFlows();
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
    }

    @Xray(test = "PEG-1976, PEG-1975, PEG-1977")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void createService(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final JSONObject serviceCreationBody = bodyBuilder();
        ServicesHelper.createService(token, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));

        ServicesHelper.createService(token, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Xray(test = "PEG-1980, PEG-1993")
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void createInactiveService(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject inactiveServiceCreationBody = bodyBuilder();
        final String serviceStatus = "INACTIVE";
        inactiveServiceCreationBody.put(STATUS, serviceStatus);

        ServicesHelper.createService(token, organizationId, inactiveServiceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(STATUS, equalTo(serviceStatus));
    }

    @Xray(test = "PEG-1981")
    @Test(dataProvider = "resourceSelection", dataProviderClass = ServiceDataProvider.class)
    public void createServiceWithDifferentResourceSelection(String resourceSelection) {
        final JSONObject inactiveServiceCreationBody = bodyBuilder();
        inactiveServiceCreationBody.put(RESOURCE_SELECTION, resourceSelection);

        ServicesHelper.createService(ownerToken, organizationId, inactiveServiceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(RESOURCE_SELECTION, equalTo(resourceSelection));
    }

    @Xray(test = "PEG-1982")
    @Test
    public void createServiceNonPublishedOrganization() {
        final String unpublishedOrganizationId = new OrganizationFlows().createUnpublishedOrganizationWithOwner()
                .getJSONObject("ORGANIZATION").getString("id");
        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(SUPPORT_TOKEN, unpublishedOrganizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));

    }

    @Xray(test = "PEG-1983")
    @Test
    public void createServiceBlockedOrganization() {
        final String blockedOrganizationId = new OrganizationFlows().createBlockedOrganizationWithAllUsers()
                .getJSONObject("ORGANIZATION").getString("id");
        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(SUPPORT_TOKEN, blockedOrganizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));
    }

    @Xray(test = "PEG-1984")
    @Test
    public void creteServicePausedOrganization() {
        final String pausedOrgId = new OrganizationFlows().createPausedOrganizationWithAllUsers()
                .getJSONObject("ORGANIZATION").getString("id");
        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(SUPPORT_TOKEN, pausedOrgId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));
    }

    @Xray(test = "PEG-1985, PEG-1986")
    @Test
    public void createServiceDeletedOrganization() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final String deletedOrgId = organizationFlows.createAndDeletePublishedOrganization()
                .getJSONObject("ORGANIZATION")
                .getString("id");
        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(SUPPORT_TOKEN, deletedOrgId, serviceCreationBody)
                .then()
                .statusCode(SC_NOT_FOUND);

        organizationFlows.restoreOrganization(deletedOrgId);

        ServicesHelper.createService(SUPPORT_TOKEN, deletedOrgId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));
    }

    @Xray(test = "PEG-1989")
    @Test
    public void createServiceDeletedOwner() {
        final JSONObject ownerProps = userFlows.createUser(organizationId, OWNER, null);
        final String ownerEmail = ownerProps.getString("email");
        final String token = new AuthenticationFlowHelper().getTokenWithEmailPassword(ownerEmail, password);

        userFlows.deleteUser(organizationId, ownerProps.getString("id"));

        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(token, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-1990")
    @Test
    public void createServiceInactiveOwner() {
        final JSONObject ownerProps = userFlows.createUser(organizationId, OWNER, null);
        final String ownerEmail = ownerProps.getString("email");
        final String token = new AuthenticationFlowHelper().getTokenWithEmailPassword(ownerEmail, password);

        userFlows.inactivateUserById(organizationId, ownerProps.getString("id"));

        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(token, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-2014")
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void createServiceWithDurationUnder5min(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;

        final JSONObject serviceCreationBody = bodyBuilder();
        final int invalidDuration = new Random().nextInt(299);
        serviceCreationBody.put(DURATION, invalidDuration);

        ServicesHelper.createService(token, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1987")
    @Test
    public void createServiceWithOtherOrganizationOwner() {

        final JSONObject otherOrganizationAndOwnerProps = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String otherOrganizationOwnerToken = otherOrganizationAndOwnerProps.getJSONObject(OWNER.name()).getString("token");

        ServicesHelper.createService(otherOrganizationOwnerToken, organizationId, bodyBuilder())
                .then()
                .statusCode(SC_FORBIDDEN);

    }

    @Xray(test = "PEG-1994")
    @Test
    public void createServicesWithSameNameDifferentOrganizations() {
        final JSONObject otherOrgProps = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String otherOrganizationId = otherOrgProps.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject serviceCreationBody = bodyBuilder();

        ServicesHelper.createService(SUPPORT_TOKEN, otherOrganizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));

        ServicesHelper.createService(SUPPORT_TOKEN, organizationId, serviceCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/createService.json"));
    }

}
