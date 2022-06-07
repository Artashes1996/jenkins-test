package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ServiceFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;
import java.util.List;

import static configuration.Role.SUPPORT;
import static configuration.Role.getRandomRole;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetServiceByIdTest extends BaseTest {

    private String organizationId;
    private String serviceId;
    private JSONObject service;
    private JSONObject organizationAndUsers;
    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;
    private ServiceFlows serviceFlows;
    private LocationFlows locationFlows;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        serviceFlows = new ServiceFlows();
        locationFlows = new LocationFlows();

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        service = serviceFlows.createService(organizationId);
        serviceId = service.getString("id");
    }

    @Test(testName = "PEG-2207, PEG-2208", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getService(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(role.name()).getString("token");
        ServicesHelper.getServiceById(token, organizationId, serviceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("visibility.webKiosk", equalTo(true))
                .body("visibility.physicalKiosk", equalTo(true))
                .body("visibility.monitor", equalTo(true))
                .body("resourceSelection", equalTo(ServiceCreationRequestBody.ResourceSelection.ALLOWED.name()))
                .body("status", equalTo(ServiceCreationRequestBody.Status.ACTIVE.name()))
                .body("linkedLocationIds", equalTo(Collections.emptyList()));
    }


    @Test(testName = "PEG-2210, PEG-2211", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getServiceOfPausedOrganization(Role role) {
        final JSONObject pausedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        final String pausedOrganizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                pausedOrganization.getJSONObject(role.name()).getString("token");
        final JSONObject pausedOrganizationService = serviceFlows.createService(pausedOrganizationId);
        final String pausedOrganizationServiceId = pausedOrganizationService.getString("id");
        ServicesHelper.getServiceById(token, pausedOrganizationId, pausedOrganizationServiceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("visibility.webKiosk", equalTo(true))
                .body("visibility.physicalKiosk", equalTo(true))
                .body("visibility.monitor", equalTo(true))
                .body("resourceSelection", equalTo(ServiceCreationRequestBody.ResourceSelection.ALLOWED.name()))
                .body("status", equalTo(ServiceCreationRequestBody.Status.ACTIVE.name()))
                .body("linkedLocationIds", equalTo(Collections.emptyList()));
    }

    @Test(testName = "PEG-3329")
    public void getServiceOfDeletedOrganization() {
        final JSONObject deletedOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String deletedOrganizationId = deletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject deletedOrganizationService = serviceFlows.createService(deletedOrganizationId);
        final String deletedOrganizationLocationId = new LocationFlows().createLocation(deletedOrganizationId).getString("id");
        final String deletedOrganizationServiceId = deletedOrganizationService.getString("id");
        serviceFlows.linkLocationsToService(deletedOrganizationId, deletedOrganizationServiceId, List.of(deletedOrganizationLocationId));
        organizationFlows.deleteOrganization(deletedOrganizationId);
        ServicesHelper.getServiceById(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationServiceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("visibility.webKiosk", equalTo(true))
                .body("visibility.physicalKiosk", equalTo(true))
                .body("visibility.monitor", equalTo(true))
                .body("resourceSelection", equalTo(ServiceCreationRequestBody.ResourceSelection.ALLOWED.name()))
                .body("status", equalTo(ServiceCreationRequestBody.Status.ACTIVE.name()))
                .body("linkedLocationIds", is(List.of(deletedOrganizationLocationId)));
    }

    @Test(testName = "PEG-2212, PEG-2213", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getServiceOfBlockedOrganization(Role role) {
        final JSONObject blockedOrganization = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String blockedOrganizationId = blockedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject blockedOrganizationService = serviceFlows.createService(blockedOrganizationId);
        final String blockedOrganizationServiceId = blockedOrganizationService.getString("id");
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :blockedOrganization.getJSONObject(role.name()).getString("token");

        ServicesHelper.getServiceById(token, blockedOrganizationId, blockedOrganizationServiceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("visibility.webKiosk", equalTo(true))
                .body("visibility.physicalKiosk", equalTo(true))
                .body("visibility.monitor", equalTo(true))
                .body("resourceSelection", equalTo(ServiceCreationRequestBody.ResourceSelection.ALLOWED.name()))
                .body("status", equalTo(ServiceCreationRequestBody.Status.ACTIVE.name()))
                .body("linkedLocationIds", equalTo(Collections.emptyList()));
    }

    @Xray(test = "PEG-6463", requirement = "PEG-6009")
    @Test
    public void getServiceWithLinkedLocations() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newServiceId = new ServiceFlows().createService(organizationId).getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");

        final List<String> linkLocationIds = List.of(locationId, newLocationId);
        serviceFlows.linkLocationsToService(organizationId, newServiceId, linkLocationIds);
        locationFlows.inactivateLocation(organizationId, linkLocationIds.get(1));

        ServicesHelper.getServiceById(token, organizationId, newServiceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("visibility.webKiosk", equalTo(true))
                .body("visibility.physicalKiosk", equalTo(true))
                .body("visibility.monitor", equalTo(true))
                .body("resourceSelection", equalTo(ServiceCreationRequestBody.ResourceSelection.ALLOWED.name()))
                .body("status", equalTo(ServiceCreationRequestBody.Status.ACTIVE.name()))
                .body("linkedLocationIds", hasSize(2))
                .body("linkedLocationIds", hasItems(linkLocationIds.get(0), linkLocationIds.get(1)));
    }

    @Xray(test = "PEG-6465", requirement = "PEG-6009")
    @Test
    public void getServiceAfterUnlinkLocation() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final String newServiceId = new ServiceFlows().createService(organizationId).getString("id");
        final String newLocationId1 = locationFlows.createLocation(organizationId).getString("id");
        final String newLocationId2 = locationFlows.createLocation(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, List.of(newLocationId1, newLocationId2));
        serviceFlows.unlinkLocationsFromService(organizationId, newServiceId, List.of(newLocationId2));

        ServicesHelper.getServiceById(token, organizationId, newServiceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("visibility.webKiosk", equalTo(true))
                .body("visibility.physicalKiosk", equalTo(true))
                .body("visibility.monitor", equalTo(true))
                .body("resourceSelection", equalTo(ServiceCreationRequestBody.ResourceSelection.ALLOWED.name()))
                .body("status", equalTo(ServiceCreationRequestBody.Status.ACTIVE.name()))
                .body("linkedLocationIds", is(List.of(newLocationId1)));
    }

    @Test(testName = "PEG-2214", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getServiceByInactiveUser(Role role) {
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject inactiveUser = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String inactiveUserToken = inactiveUser.getString("token");
        userFlows.inactivateUserById(organizationId, inactiveUser.getString("id"));
        ServicesHelper.getServiceById(inactiveUserToken, organizationId, serviceId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test(testName = "PEG-2215", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getServiceByDeletedUser(Role role) {
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject deletedUser = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String deletedUserToken = deletedUser.getString("token");
        userFlows.deleteUser(organizationId, deletedUser.getString("id"));
        ServicesHelper.getServiceById(deletedUserToken, organizationId, serviceId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test(testName = "PEG-2216", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getServiceByOtherOrganizationUser(Role role) {
        final JSONObject otherOrganization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String otherLocationId = otherOrganization.getJSONObject("LOCATION").getString("id");
        final String otherOrganizationId = otherOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject otherUser = userFlows.createUser(otherOrganizationId, role, Collections.singletonList(otherLocationId));
        final String otherUserToken = otherUser.getString("token");
        ServicesHelper.getServiceById(otherUserToken, organizationId, serviceId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

}
