package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.serviceresource.data.ServiceDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.LinkUnlinkLocationsToServiceRequestBody;
import helpers.appsapi.servicesresource.payloads.LinkawareSearchServiceRequestBody;
import helpers.flows.*;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;
import java.util.stream.IntStream;

import static configuration.Role.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.*;


public class LinkLocationsToServiceTest extends BaseTest {

    private static String organizationId;
    private static String serviceId;
    private static String locationIdForLocationAdminUser;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private ServiceFlows serviceFlows;
    private JSONObject organizationAndUsersObject;
    private AuthenticationFlowHelper authenticationFlowHelper;
    private final String NOT_READABLE_REQUEST_BODY = "NOT_READABLE_REQUEST_BODY";

    @BeforeClass
    public void organizationDataPreparation() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();
        authenticationFlowHelper = new AuthenticationFlowHelper();
        organizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        locationIdForLocationAdminUser = organizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        organizationId = organizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
    }

    @BeforeMethod
    public void serviceDataPreparation() {
        serviceId = serviceFlows.createService(organizationId).getString("id");
    }


    @Xray(test = "PEG-3397")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkServiceToEmptyLocationList(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray()))
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("types[0]", equalTo("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-3398")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkWithEmptyPayload(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId, new JSONObject())
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .assertThat()
                .body("types[0]", equalTo("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-3402")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkWithIncorrectActionEnum(Role role) {
        final JSONObject linkBody = new JSONObject();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        linkBody.put(LinkUnlinkLocationsToServiceRequestBody.ACTION, false);
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId, linkBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .assertThat()
                .body("types[0]", equalTo(NOT_READABLE_REQUEST_BODY));
        linkBody.put(LinkUnlinkLocationsToServiceRequestBody.ACTION, new JSONObject());
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId, linkBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .assertThat()
                .body("types[0]", equalTo(NOT_READABLE_REQUEST_BODY));

    }

    @Xray(test = "PEG-3403")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkWithIncorrectLocation(Role role) {
        final JSONObject linkBody = new JSONObject();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        linkBody.put(LinkUnlinkLocationsToServiceRequestBody.ACTION, LINK);
        linkBody.put(LinkUnlinkLocationsToServiceRequestBody.LOCATION_IDS, false);
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId, linkBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .assertThat()
                .body("types[0]", equalTo(NOT_READABLE_REQUEST_BODY));
        linkBody.put(LinkUnlinkLocationsToServiceRequestBody.LOCATION_IDS, new JSONObject());
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId, linkBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .assertThat()
                .body("types[0]", equalTo(NOT_READABLE_REQUEST_BODY));
    }

    @Xray(test = "PEG-3406")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void linkLocationsToNonExistingService(Role role) {
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String fakeServiceId = UUID.randomUUID().toString();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, fakeServiceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray().put(locationId)))
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .assertThat()
                .body("types[0]", equalTo("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-3613")
    @Test
    public void linkLocationsToNonExistingServiceLocationSupport() {
        final String fakeServiceId = UUID.randomUUID().toString();
        ServicesHelper.linkUnlinkLocationsToService(SUPPORT_TOKEN, organizationId, fakeServiceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray().put(locationIdForLocationAdminUser)))
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .assertThat()
                .body("types[0]", equalTo("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-3409")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void linkOperationInNonExistingOrganization(Role role) {
        final String locationId = locationFlows.createLocation(organizationId).getString("id");
        final String fakeOrganizationId = UUID.randomUUID().toString();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        ServicesHelper.linkUnlinkLocationsToService(token, fakeOrganizationId, serviceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray().put(locationId)))
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .assertThat()
                .body("types[0]", equalTo("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3411")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void linkOperationInOtherOrganization(Role role) {
        final String newOrganizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final String token = authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        final JSONArray locations = new JSONArray().put(locationIdForLocationAdminUser);
        final JSONObject linkRequestBody = LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, locations);

        ServicesHelper.linkUnlinkLocationsToService(token, newOrganizationId, serviceId, linkRequestBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .assertThat()
                .body("types[0]", equalTo("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3614")
    @Test
    public void linkOperationInOtherOrganizationSupport() {
        final String newOrganizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final JSONArray locations = new JSONArray().put(locationIdForLocationAdminUser);
        final JSONObject linkRequestBody = LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, locations);

        ServicesHelper.linkUnlinkLocationsToService(SUPPORT_TOKEN, newOrganizationId, serviceId, linkRequestBody)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("types[0]", equalTo("RESOURCE_NOT_FOUND"))
                .body("messages[0]", containsString("No Service can be found by given"));
    }

    @Xray(test = "PEG-3396, PEG-3457")
    @Test(dataProvider = "roles with location index", dataProviderClass = ServiceDataProvider.class)
    public void linkOperationWithDifferentRoles(Role role, int locationIdIndex) {
        final int locationsCount = 4;
        final JSONArray locationIds = locationFlows.createLocations(organizationId, locationsCount);
        final JSONArray linkBody = role.equals(LOCATION_ADMIN) ? new JSONArray().put(locationIdForLocationAdminUser) :
                new JSONArray().put(locationIds.getJSONObject(locationIdIndex).getString("id"));
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, linkBody))
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    @SneakyThrows
    @Xray(test = "PEG-3413")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkSingleLocationToService(Role role) {
        final JSONObject searchBody = LinkawareSearchServiceRequestBody.bodyBuilder();
        searchBody.put(LinkawareSearchServiceRequestBody.QUERY, locationIdForLocationAdminUser);
        final JSONArray linkBody = new JSONArray().put(locationIdForLocationAdminUser);
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, linkBody))
                .then()
                .statusCode(HttpStatus.SC_OK);

        Thread.sleep(500);
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("content.id", hasItem(locationIdForLocationAdminUser))
                .body("content.linked", hasItem(true));

        ServicesHelper.linkUnlinkLocationsToService(token, organizationId, serviceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(UNLINK, new JSONArray().put(locationIdForLocationAdminUser)))
                .then()
                .statusCode(HttpStatus.SC_OK);

        Thread.sleep(500);
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("content.id", hasItem(locationIdForLocationAdminUser))
                .body("content.linked", hasItem(false));
    }

    @Xray(test = "PEG-3415")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkLocationsToService(Role role) {
        final JSONObject newOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithOwner();
        final String newOrganizationId = newOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final JSONArray createdLocations = locationFlows.createLocations(newOrganizationId, 12);
        final String newServiceId = serviceFlows.createService(newOrganizationId).getString("id");
        final ArrayList<String> locationIds = new ArrayList<>();
        final JSONObject searchBody = LinkawareSearchServiceRequestBody.bodyBuilder();
        searchBody.getJSONObject(LinkawareSearchServiceRequestBody.PAGINATION).put(LinkawareSearchServiceRequestBody.SIZE, 12);
        IntStream.range(0, createdLocations.length()).mapToObj(i -> createdLocations.getJSONObject(i).getString("id"))
                .forEach(locationIds::add);
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getUserTokenByRole(newOrganizationId, role, locationIds);
        ServicesHelper.linkUnlinkLocationsToService(token, newOrganizationId, newServiceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray(locationIds)))
                .then()
                .statusCode(HttpStatus.SC_OK);

        final ArrayList<String> foundLocations = ServicesHelper.searchForLinkedLocations(token, newOrganizationId, newServiceId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .extract()
                .path("content.id");

        Collections.sort(locationIds);
        Collections.sort(foundLocations);
        assertEquals(locationIds, foundLocations);

        locationIds.remove(0);
        locationIds.remove(7);
        locationIds.remove(3);

        ServicesHelper.linkUnlinkLocationsToService(token, newOrganizationId, newServiceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(UNLINK, new JSONArray(locationIds)))
                .then()
                .statusCode(HttpStatus.SC_OK);

        final ArrayList<String> foundPartOfLocations = ServicesHelper.searchForLinkedLocations(token, newOrganizationId, newServiceId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .extract()
                .path("content.findAll{it.linked==false}.id");

        Collections.sort(foundPartOfLocations);
        assertEquals(foundPartOfLocations, locationIds);
    }

}
