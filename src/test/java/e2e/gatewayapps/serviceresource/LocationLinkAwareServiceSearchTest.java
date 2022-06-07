package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.serviceresource.data.ServiceDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.LinkUnlinkLocationsToServiceRequestBody;
import helpers.appsapi.servicesresource.payloads.LinkawareSearchServiceRequestBody;
import helpers.flows.*;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertEquals;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.LINK;

public class LocationLinkAwareServiceSearchTest extends BaseTest {

    private final OrganizationFlows organizationFlows = new OrganizationFlows();
    private JSONObject organizationAndUsers;
    private ArrayList<JSONObject> locations;
    private final ArrayList<String> locationIds = new ArrayList<>();
    private String serviceId;
    private String ownerToken;
    private String organizationId;
    private final Random random = new Random();

    @BeforeClass
    public void dataPreparation() {
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final LocationFlows locationFlows = new LocationFlows();
        locations = new ArrayList<>();
        locations.add(organizationAndUsers.getJSONObject("LOCATION"));
        locations.add(locationFlows.createInactiveLocation(organizationId));
        locations.add(locationFlows.createLocation(organizationId));
        locationIds.add(locations.get(0).getString("id"));
        locationIds.add(locations.get(1).getString("id"));
        for (int i = 0; i < 6; i++) {
            final JSONObject location = locationFlows.createLocation(organizationId);
            locations.add(location);
            locationIds.add(location.getString("id"));
        }
        final ServiceFlows serviceFlows = new ServiceFlows();
        serviceId = serviceFlows.createService(organizationId).getString("id");

        ServicesHelper.linkUnlinkLocationsToService(ownerToken, organizationId, serviceId,
                LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, new JSONArray(locationIds)))
                .then()
                .statusCode(HttpStatus.SC_OK);
    }

    // TODO This test in non properly designed, please review and make the changes.
    @Xray(test = "PEG-2172")
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void locationServiceLinkAware(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;

        final ArrayList<String> contentLocationIds = ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .extract()
                .path("content.findAll{it.linked==true}.id");

        Collections.sort(contentLocationIds);
        Collections.sort(locationIds);
        assertEquals(locationIds, contentLocationIds);
    }

    @Xray(test = "PEG-2173")
    @Test
    public void locationAwareSearchOtherOrganizationService() {
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");
        final String otherOrgServiceId = new ServiceFlows().createService(otherOrganizationId).getString("id");
        ServicesHelper.searchForLinkedLocations(ownerToken, organizationId, otherOrgServiceId, new JSONObject())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    // TODO This test in non properly designed, please review and make the changes.
    @Test(testName = "PEG-2174", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 20)
    public void searchForLocationLinkBlockedOrganization(Role role) {
        final String token = role.equals(OWNER) ? ownerToken : SUPPORT_TOKEN;
        final JSONObject searchEmptyBody = new JSONObject();

        organizationFlows.blockOrganization(organizationId);

        final ArrayList<String> contentLocationIds = ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, searchEmptyBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .extract()
                .path("content.findAll{it.linked==true}.id");

        Collections.sort(contentLocationIds);
        Collections.sort(locationIds);

        assertEquals(locationIds,contentLocationIds);

        organizationFlows.unblockOrganization(organizationId);
    }

    // TODO This test in non properly designed, please review and make the changes.
    @Test(testName = "PEG-2175", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void searchForLocationLinkPausedOrganization(Role role) {
        final String token = role.equals(OWNER) ? ownerToken : SUPPORT_TOKEN;
        final JSONObject searchEmptyBody = new JSONObject();

        organizationFlows.pauseOrganization(organizationId);

        final ArrayList<String> contentLocationIds = ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, searchEmptyBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .extract()
                .path("content.findAll{it.linked==true}.id");

        Collections.sort(contentLocationIds);
        Collections.sort(locationIds);
        assertEquals(locationIds,contentLocationIds);

        organizationFlows.unpauseOrganization(organizationId);
    }

    // TODO with parallel threads you cannot delete organization and put priority change test
    @Test(enabled = false, testName = "PEG-2176")
    public void searchForLocationLinkDeletedOrganization() {
        final JSONObject searchEmptyBody = new JSONObject();
        organizationFlows.deleteOrganization(organizationId);
        ServicesHelper.searchForLinkedLocations(SUPPORT_TOKEN, organizationId, serviceId, searchEmptyBody)
                .then()
                .statusCode(SC_NOT_FOUND);
        organizationFlows.restoreOrganization(organizationId);
    }

    @Test(testName = "PEG-2177")
    public void searchForLinkAwareUnpublishedOrganization() {
        final JSONObject organization = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION");
        final LocationFlows locationFlows = new LocationFlows();
        final JSONArray locationIds = new JSONArray();
        locationIds.put(locationFlows.createLocation(organization.getString("id")).getString("id"));
        ServiceFlows serviceFlows = new ServiceFlows();
        String serviceId = serviceFlows.createService(organization.getString("id")).getString("id");

        final JSONObject linkRequestBody = LinkUnlinkLocationsToServiceRequestBody.bodyBuilder(LINK, locationIds);

        ServicesHelper.linkUnlinkLocationsToService(SUPPORT_TOKEN, organization.getString("id"), serviceId, linkRequestBody)
                .then()
                .statusCode(HttpStatus.SC_OK);

        final JSONObject searchEmptyBody = new JSONObject();
        ServicesHelper.searchForLinkedLocations(SUPPORT_TOKEN, organization.getString("id"), serviceId, searchEmptyBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-2178", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void searchForLinkAwareNonExistingServiceLocations(Role role) {
        final String token = role.equals(OWNER) ? ownerToken : SUPPORT_TOKEN;
        final JSONObject searchEmptyBody = new JSONObject();
        final String nonExistingServiceId = UUID.randomUUID().toString();
        ServicesHelper.searchForLinkedLocations(token, organizationId, nonExistingServiceId, searchEmptyBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-2179", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void searchForNotLinkedService(Role role) {
        final String token = role.equals(OWNER) ? ownerToken : SUPPORT_TOKEN;
        final ServiceFlows serviceFlows = new ServiceFlows();
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        final JSONObject emptyBody = new JSONObject();
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, emptyBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEF-2180")
    public void linkAwareSearchOtherOrgOwner() {
        final JSONObject otherOrganizationOwner = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject(OWNER.name());
        final String token = otherOrganizationOwner.getString("token") ;
        final JSONObject emptyBody = new JSONObject();
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, emptyBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-2183")
    public void linkAwareSearchInactiveOwner() {
        UserFlows userFlows = new UserFlows();
        final JSONObject inactiveOwner = userFlows.createUser(organizationId, OWNER, null);
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(inactiveOwner.getString("email"));
        userFlows.inactivateUserById(organizationId, inactiveOwner.getString("id"));
        final JSONObject emptyBody = new JSONObject();
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, emptyBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test(testName = "PEG-2184")
    public void linkAwareSearchWithDeletedOwner() {
        UserFlows userFlows = new UserFlows();
        final JSONObject deletedOwner = userFlows.createUser(organizationId, OWNER, null);
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(deletedOwner.getString("email"));
        userFlows.deleteUser(organizationId, deletedOwner.getString("id"));
        final JSONObject emptyBody = new JSONObject();
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, emptyBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }



    @Test(testName = "PEG-2196", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void linkAwareSearchInactiveLocations(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject searchBody = new JSONObject();
        final JSONArray statuses = new JSONArray();
        final String status = "INACTIVE";
        statuses.put(status);
        searchBody.put(LinkawareSearchServiceRequestBody.STATUSES, statuses);
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.status}", everyItem(equalTo(status)));
    }

    @Test(testName = "PEG-2197", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void linkAwareSearchActiveLocations(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject searchBody = new JSONObject();
        final JSONArray statuses = new JSONArray();
        final String status = "ACTIVE";
        statuses.put("ACTIVE");
        searchBody.put(LinkawareSearchServiceRequestBody.STATUSES, statuses);
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.status}", everyItem(equalTo(status)));
    }

    @Test(testName = "PEG-2219", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void filterInactiveLocations(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject filterBody = new JSONObject();
        final String status = "INACTIVE";
        final JSONArray statuses = new JSONArray();
        statuses.put(status);
        filterBody.put(LinkawareSearchServiceRequestBody.STATUSES, statuses);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.status}", everyItem(equalTo(status)));
    }

    @Xray(test = "PEG-2222")
    @Test(dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void filterActiveLocationsSameFilterTwice(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject filterBody = new JSONObject();
        final String status = "ACTIVE";
        final JSONArray statuses = new JSONArray();
        statuses.put(status);
        statuses.put(status);
        filterBody.put(LinkawareSearchServiceRequestBody.STATUSES, statuses);
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.status}", everyItem(equalTo(status)));
    }

    @Test(testName = "PEG-2221", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void filterLocationsByStatusCity(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject filterBody = new JSONObject();
        final String status = random.nextInt(10) % 2 == 0 ? "ACTIVE" : "INACTIVE";

        final List<String> citiesList = locations.stream().filter(location -> location.getString("status").equals(status))
                .map(location -> location.getJSONObject("address").getString("city"))
                .collect(Collectors.toList());

        final String city = citiesList.get(random.nextInt(citiesList.size()));
        final JSONArray statuses = new JSONArray();
        final JSONArray cities = new JSONArray();
        statuses.put(status);
        cities.put(city);
        filterBody.put(LinkawareSearchServiceRequestBody.STATUSES, statuses);
        filterBody.put(LinkawareSearchServiceRequestBody.CITIES, cities);
        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.status}", everyItem(equalTo(status)))
                .body("content.collect{it.address.city}", everyItem(equalTo(city)));
    }

    @Test(testName = "PEG-2223", dataProvider = "invalidFilterType", dataProviderClass = ServiceDataProvider.class)
    public void invalidFilterTypesByOwner(Object filterTypes) {
        final JSONObject filterBody = new JSONObject();

        final JSONArray invalidFilterType = new JSONArray();
        invalidFilterType.put(filterTypes);

        filterBody.put(LinkawareSearchServiceRequestBody.CITIES, invalidFilterType);
        ServicesHelper.searchForLinkedLocations(ownerToken, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
        filterBody.remove(LinkawareSearchServiceRequestBody.CITIES);

        filterBody.put(LinkawareSearchServiceRequestBody.STATE_REGIONS, invalidFilterType);
        ServicesHelper.searchForLinkedLocations(ownerToken, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
        filterBody.remove(LinkawareSearchServiceRequestBody.STATE_REGIONS);

        filterBody.put(LinkawareSearchServiceRequestBody.ZIPCODES, invalidFilterType);
        ServicesHelper.searchForLinkedLocations(ownerToken, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test(testName = "PEG-2223", dataProvider = "invalidFilterType", dataProviderClass = ServiceDataProvider.class)
    public void invalidFilterTypesBySupport(Object filterTypes) {
        final JSONObject filterBody = new JSONObject();

        final JSONArray invalidFilterType = new JSONArray();
        invalidFilterType.put(filterTypes);

        filterBody.put(LinkawareSearchServiceRequestBody.CITIES, invalidFilterType);
        ServicesHelper.searchForLinkedLocations(ownerToken, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
        filterBody.remove(LinkawareSearchServiceRequestBody.CITIES);

        filterBody.put(LinkawareSearchServiceRequestBody.STATE_REGIONS, invalidFilterType);
        ServicesHelper.searchForLinkedLocations(ownerToken, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
        filterBody.remove(LinkawareSearchServiceRequestBody.STATE_REGIONS);

        filterBody.put(LinkawareSearchServiceRequestBody.ZIPCODES, invalidFilterType);
        ServicesHelper.searchForLinkedLocations(SUPPORT_TOKEN, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test(testName = "PEG-2226", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void filterByZipCode(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final List<String> zipCodeList = locations.stream()
                .map(location -> location.getJSONObject("address").getString("zipcode")).collect(Collectors.toList());
        final JSONObject filterBody = new JSONObject();
        final JSONArray zipCodes = new JSONArray();
        zipCodes.put(zipCodeList.get(0));
        zipCodes.put(zipCodeList.get(1));
        filterBody.put(LinkawareSearchServiceRequestBody.ZIPCODES, zipCodes);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.address.zipcode}", hasItem(zipCodeList.get(0)))
                .body("content.collect{it.address.zipcode}", hasItem(zipCodeList.get(1)));
    }

    @Test(testName = "PEG-2224", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void filterByCities(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final List<String> citiesList = locations.stream()
                .map(location -> location.getJSONObject("address").getString("city")).collect(Collectors.toList());
        final JSONObject filterBody = new JSONObject();
        final JSONArray cities = new JSONArray();
        cities.put(citiesList.get(0));
        cities.put(citiesList.get(1));
        filterBody.put(LinkawareSearchServiceRequestBody.CITIES, cities);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.address.city}", hasItem(citiesList.get(0)))
                .body("content.collect{it.address.city}", hasItem(citiesList.get(1)));
    }

    @Test(testName = "PEG-2225", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class)
    public void filterByStateRegion(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final List<String> regionsList = locations.stream()
                .map(location -> location.getJSONObject("address").getString("stateRegion")).collect(Collectors.toList());
        final JSONObject filterBody = new JSONObject();
        final JSONArray regions = new JSONArray();
        regions.put(regionsList.get(0));
        regions.put(regionsList.get(1));

        filterBody.put(LinkawareSearchServiceRequestBody.STATE_REGIONS, regions);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.address.stateRegion}", hasItem(regionsList.get(0)))
                .body("content.collect{it.address.stateRegion}", hasItem(regionsList.get(1)));
    }

    @Xray(test = "PEG-6417", requirement = "PEG-6010")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void filterByLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject filterBody = new JSONObject();
        final String notLinkedLocationId = locations.get(2).getString("id");
        final List<String> requestedLocationIds = List.of(locationIds.get(0), notLinkedLocationId);
        filterBody.put(LinkawareSearchServiceRequestBody.LOCATION_IDS, requestedLocationIds);

        final String linkPathTemplate = "content.find{it.id=='%s'}.linked";

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content", hasSize(2))
                .body("content.collect{it.id}", hasItems(requestedLocationIds.get(0), requestedLocationIds.get(1)))
                .body(String.format(linkPathTemplate, requestedLocationIds.get(0)), is(true))
                .body(String.format(linkPathTemplate, requestedLocationIds.get(1)), is(false));
    }

    @Xray(test = "PEG-6423", requirement = "PEG-6010")
    @Test
    public void filterByOtherOrganizationLocation() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject otherOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String notRelatedLocationId = otherOrganizationAndUsers.getJSONObject("LOCATION").getString("id");
        final List<String> requestedLocationIds = List.of(notRelatedLocationId);
        final JSONObject filterBody = new JSONObject();
        filterBody.put(LinkawareSearchServiceRequestBody.LOCATION_IDS, requestedLocationIds);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content", equalTo(Collections.emptyList()));
    }

    @Xray(test = "PEG-6425", requirement = "PEG-6010")
    @Test
    public void filterByNonExistingLocation() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject filterBody = new JSONObject();
        final List<String> requestedLocationIds = List.of("NonExistingId");
        filterBody.put(LinkawareSearchServiceRequestBody.LOCATION_IDS, requestedLocationIds);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content", equalTo(Collections.emptyList()));
    }

    @Xray(test = "PEG-6440", requirement = "PEG-6010")
    @Test
    public void filterByLocationsStatusesAndZipcodes() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject filterBody = new JSONObject();

        final List<String> requestedLocationIds = IntStream.range(0,3).mapToObj(locationIds::get).collect(Collectors.toList());

        final String activeLocationZipcode = locations.get(0).getJSONObject("address").getString("zipcode");
        final String inactiveLocationZipcode = locations.get(1).getJSONObject("address").getString("zipcode");
        final JSONArray zipcodes = new JSONArray().put(activeLocationZipcode).put(inactiveLocationZipcode);

        filterBody.put(LinkawareSearchServiceRequestBody.LOCATION_IDS, requestedLocationIds);
        filterBody.put(LinkawareSearchServiceRequestBody.ZIPCODES, zipcodes);
        filterBody.put(LinkawareSearchServiceRequestBody.STATUSES, new JSONArray().put("ACTIVE"));

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.id}", is(List.of(requestedLocationIds.get(0))));
    }

    @Xray(test = "PEG-6441", requirement = "PEG-6010")
    @Test
    public void filterByLocationsCitiesAndStateRegions() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject filterBody = new JSONObject();

        final List<String> requestedLocationIds = IntStream.range(0,4)
                .mapToObj(i -> locations.get(i).getString("id")).collect(Collectors.toList());

        final List<String> cities = IntStream.range(0,3).mapToObj(i -> locations.get(i).getJSONObject("address"))
                        .map( e -> e.getString("city")).collect(Collectors.toList());

        final List<String> stateRegions = IntStream.range(1,4).mapToObj(i -> locations.get(i).getJSONObject("address"))
                .map( e -> e.getString("stateRegion")).collect(Collectors.toList());

        filterBody.put(LinkawareSearchServiceRequestBody.CITIES, cities);
        filterBody.put(LinkawareSearchServiceRequestBody.STATE_REGIONS, stateRegions);
        filterBody.put(LinkawareSearchServiceRequestBody.LOCATION_IDS, requestedLocationIds);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content", hasSize(2))
                .body("content.collect{it.id}", containsInAnyOrder(requestedLocationIds.get(1), requestedLocationIds.get(2)));

    }

    @Xray(test = "PEG-6442", requirement = "PEG-6010")
    @Test
    public void filterByLocationsAndQuery() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject filterBody = new JSONObject();

        final List<String> requestedLocationIds = List.of(locationIds.get(0), locationIds.get(1));
        final String query = locations.get(0).getJSONObject("address").getString("country");

        filterBody.put(LinkawareSearchServiceRequestBody.LOCATION_IDS, requestedLocationIds);
        filterBody.put(LinkawareSearchServiceRequestBody.QUERY, query);

        ServicesHelper.searchForLinkedLocations(token, organizationId, serviceId, filterBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationAwareServices.json"))
                .body("content.collect{it.id}", is(List.of(requestedLocationIds.get(0))));
    }
}

