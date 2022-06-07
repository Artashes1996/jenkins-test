package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.serviceresource.data.ServiceDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.ServiceSearchRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ServiceFlows;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static configuration.Role.*;
import static helpers.appsapi.servicesresource.payloads.ServiceSearchRequestBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;
import static utils.commons.ToggleAction.LINK;

public class ServiceSearchTest {

    private OrganizationFlows organizationFlows;
    private String organizationId;
    private String locationId;
    private JSONObject organizationAndUsers;

    private List<JSONObject> services;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final ServiceFlows serviceFlows = new ServiceFlows();
        services = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            services.add(serviceFlows.createService(organizationId));
        }

    }

    @Xray(test = "PEG-3429", requirement = "PEG-2826")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchServiceByName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(ServiceSearchRequestBody.SearchServiceLocationCombination.REQUIRED_FIELDS);
        final String serviceName = services.get(0).getString("internalName");
        searchBody.put(QUERY, serviceName);
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));

        searchBody.put(QUERY, serviceName.substring(5));
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));

        searchBody.put(QUERY, serviceName.substring(4, 8));
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
    }

    @Xray(test = "PEG-3430", requirement = "PEG-2826")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchServiceById(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(ServiceSearchRequestBody.SearchServiceLocationCombination.REQUIRED_FIELDS);
        final JSONObject service = services.get(getRandomInt(services.size() - 1));
        final String serviceId = service.getString("id");
        searchBody.put(QUERY, serviceId);
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.id", hasItem(serviceId));

        searchBody.put(QUERY, serviceId.substring(5));
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.id", hasItem(serviceId));

        searchBody.put(QUERY, serviceId.substring(4, 8));
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.id", hasItem(serviceId));
    }

    @Xray(test = "PEG-3431", requirement = "PEG-2826")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void searchServiceByOtherOrganizationUser(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(ServiceSearchRequestBody.SearchServiceLocationCombination.REQUIRED_FIELDS);
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-3432", requirement = "PEG-2826")
    @Test
    public void searchInPausedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final String serviceName = new ServiceFlows().createService(organizationId).getString("internalName");

        organizationFlows.pauseOrganization(organizationId);
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(ServiceSearchRequestBody.SearchServiceLocationCombination.REQUIRED_FIELDS);
        ServicesHelper.sortAndSearchService(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
        ServicesHelper.sortAndSearchService(adminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
        ServicesHelper.sortAndSearchService(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName));
        ServicesHelper.sortAndSearchService(staffToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName));
    }

    @Xray(test = "PEG-3433", requirement = "PEG-2826")
    @Test
    public void searchServiceInBlockedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final String serviceName = new ServiceFlows().createService(organizationId).getString("internalName");

        organizationFlows.blockOrganization(organizationId);
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(ServiceSearchRequestBody.SearchServiceLocationCombination.REQUIRED_FIELDS);
        ServicesHelper.sortAndSearchService(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName));
        ServicesHelper.sortAndSearchService(adminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName));
        ServicesHelper.sortAndSearchService(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
        ServicesHelper.sortAndSearchService(staffToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(1))
                .body("content.internalName", hasItem(serviceName))
                .body(matchesJsonSchemaInClasspath("schemas/searchService.json"));
    }

    @Xray(test = "PEG-3434", requirement = "PEG-2826")
    @Test
    public void searchInDeletedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");

        organizationFlows.deleteOrganization(organizationId);
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(ServiceSearchRequestBody.SearchServiceLocationCombination.REQUIRED_FIELDS);
        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK);
        ServicesHelper.sortAndSearchService(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        ServicesHelper.sortAndSearchService(adminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        ServicesHelper.sortAndSearchService(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        ServicesHelper.sortAndSearchService(staffToken, organizationId, searchBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-3435", requirement = "PEG-2826")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void sortServicesByValidValues(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(SearchServiceLocationCombination.WITH_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.INTERNAL_NAME.getAscending())
                .put(SIZE, services.size());
        final List<String> internalNames = services.stream().map(services -> services.getString("internalName")).sorted().collect(Collectors.toList());
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", equalTo(internalNames));

        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.INTERNAL_NAME.getDescending());
        internalNames.sort(Collections.reverseOrder());
        ServicesHelper.sortAndSearchService(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.internalName", equalTo(internalNames));
    }

    @Xray(test = "PEG-3436", requirement = "PEG-2826")
    @Test(dataProvider = "invalidSortValues", dataProviderClass = ServiceDataProvider.class)
    public void sortServicesByInValidValues(Object invalidSortValue) {
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(SearchServiceLocationCombination.WITH_PAGINATION);
        searchBody.getJSONObject(PAGINATION).put(SORT, invalidSortValue);
        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-3437", requirement = "PEG-2826")
    @Test
    public void checkPagination() {
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(SearchServiceLocationCombination.WITH_PAGINATION);
        final int size = 30;
        searchBody.getJSONObject(PAGINATION).put(PAGE, 1).put(SIZE, 30);

        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size()", is(services.size() - size));
    }

    @Xray(test = "PEG-2076", requirement = "PEG-2826")
    @Test
    public void checkOrderWithNonExistingOrgId() {
        final String nonExistingOrgId = UUID.randomUUID().toString();
        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, nonExistingOrgId, new JSONObject())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-7142", requirement = "PEG-7029")
    @Test
    public void searchServicesUsingInvalidLocationId() {
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(SearchServiceLocationCombination.REQUIRED_FIELDS);
        final String invalidLocationId = UUID.randomUUID().toString();
        searchBody.put(LOCATION_ID, invalidLocationId);
        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content", Matchers.empty());

    }

    @Xray(test = "PEG-7145", requirement = "PEG-7029")
    @Test
    public void searchServicesUsingLocationId() {
       final List<String> serviceIds = Stream.of(services.get(0), services.get(1))
                .map((JSONObject obj) -> obj.getString("id"))
                .collect(Collectors.toList());
        final JSONObject searchBody = ServiceSearchRequestBody.bodyBuilder(SearchServiceLocationCombination.REQUIRED_FIELDS);
        searchBody.put(LOCATION_ID, locationId);
        final LocationFlows locationFlows = new LocationFlows();
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, serviceIds, LINK);
        ServicesHelper.sortAndSearchService(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK).
                body("content.size()", is(2))
                .body("content.id", hasItems(serviceIds.get(0),serviceIds.get(1)));
    }
}
