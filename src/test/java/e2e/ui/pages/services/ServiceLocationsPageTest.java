package e2e.ui.pages.services;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ServiceFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.ServiceLocationsPage;
import pages.SignInPage;
import utils.Xray;
import utils.commons.ToggleAction;

import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;

import pages.ServiceLocationsPage.Tabs;

public class ServiceLocationsPageTest extends BasePageTest {

    private ServiceFlows serviceFlows;
    private LocationFlows locationFlows;
    private OrganizationFlows organizationFlows;
    private JSONObject ownerWithLocations;
    private JSONObject adminWithLocations;
    private JSONObject locationAdminWithLocations;
    private JSONObject staffWithLocations;
    private JSONObject service;
    private List<JSONObject> allLocations;
    private List<JSONObject> linkedLocationsToService;
    private List<JSONObject> locationsLinkedToLocationAdmin;
    private List<JSONObject> notLinkedLocationsToService;
    private String organizationId;
    private String serviceId;

    @BeforeClass
    public void setUp() {
        serviceFlows = new ServiceFlows();
        locationFlows = new LocationFlows();
        UserFlows userFlows = new UserFlows();
        organizationFlows = new OrganizationFlows();
        JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONArray locations = locationFlows.createLocations(organizationId, 4);
        final JSONObject inactiveLocation = locationFlows.createInactiveLocation(organizationId);
        locations.put(inactiveLocation);
        allLocations = IntStream.range(0, locations.length()).mapToObj(locations::getJSONObject).sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName"))).collect(Collectors.toList());
        final List<JSONObject> locationsLinkedToOwner = Stream.of(allLocations.get(0), allLocations.get(2)).sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName"))).collect(Collectors.toList());
        ownerWithLocations = userFlows.createUser(organizationId, OWNER, locationsLinkedToOwner.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        final List<JSONObject> locationsLinkedToAdmin = Stream.of(allLocations.get(0), allLocations.get(2)).sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName"))).collect(Collectors.toList());
        adminWithLocations = userFlows.createUser(organizationId, Role.ADMIN, locationsLinkedToAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        locationsLinkedToLocationAdmin = Stream.of(allLocations.get(0), allLocations.get(1), allLocations.get(4)).sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName"))).collect(Collectors.toList());
        locationAdminWithLocations = userFlows.createUser(organizationId, Role.LOCATION_ADMIN, locationsLinkedToLocationAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        staffWithLocations = userFlows.createUser(organizationId, STAFF, Collections.singletonList(allLocations.get(0).getString("id")));
        service = serviceFlows.createService(organizationId);
        serviceId = service.getString("id");
        notLinkedLocationsToService = Stream.of(allLocations.get(0), allLocations.get(1), allLocations.get(3)).sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName"))).collect(Collectors.toList());
        linkedLocationsToService = Stream.of(allLocations.get(2), allLocations.get(4)).sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName"))).collect(Collectors.toList());
        serviceFlows.linkLocationsToService(organizationId, serviceId, List.of(linkedLocationsToService.get(0).getString("id"), linkedLocationsToService.get(1).getString("id")));
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6576")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void seeListOfServiceLocationsByExtendedAdminRoles(Role role) {
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") : (role.equals(ADMIN) ? adminWithLocations.getString("token") : supportToken);
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, serviceId, userToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.checkServiceBreadCrumbItemName(service.getString("internalName"));
        serviceLocationsPage.checkLocationsInListByAdminRoles(linkedLocationsToService, notLinkedLocationsToService);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6600")
    @Test
    public void seeLocationsByLocationAdmin() {
        final String locationAdminUserToken = locationAdminWithLocations.getString("token");
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, serviceId, locationAdminUserToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.checkLocationsInListByLocationAdminRole(locationsLinkedToLocationAdmin, linkedLocationsToService);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6583")
    @Test
    public void seeListOfServiceLocationsByStaff() {
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, serviceId, staffWithLocations.getString("token"));
        serviceLocationsPage.openPage();
        serviceLocationsPage.checkLocationLinkUnlinkColumnMissing();
        serviceLocationsPage.checkLocationsInListByStaff(linkedLocationsToService);
    }


    @Xray(requirement = "PEG-4792", test = "PEG-6584")
    @Test
    public void seeEmptyLocationPage() {
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, newServiceId, staffWithLocations.getString("token"));
        serviceLocationsPage.openPage();
        serviceLocationsPage.checkLocationsCount(0);
        serviceLocationsPage.checkEmptyLocationPage();
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6585")
    @Test
    public void checkPagination() {
        final int locationsCount = 51;
        final JSONObject newOrganizationWithFiftyLocations = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String newOrganizationId = newOrganizationWithFiftyLocations.getJSONObject("ORGANIZATION").getString("id");
        final String ownerToken = newOrganizationWithFiftyLocations.getJSONObject(OWNER.name()).getString("token");
        locationFlows.createLocations(newOrganizationId, locationsCount);
        final String newServiceId = serviceFlows.createService(newOrganizationId).getString("id");
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, newOrganizationId, newServiceId, ownerToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.checkLocationsCountOfServiceInPaginationText(locationsCount);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6586")
    @Test
    public void sortByLocationNames() {
        final Role role = getRandomAdminRole();
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") : (role.equals(ADMIN) ? adminWithLocations.getString("token") : supportToken);
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, serviceId, userToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.clickOnSortingIcon();
        final List<JSONObject> newListAllLocations = new ArrayList<>(List.copyOf(allLocations));
        serviceLocationsPage.checkIfLocationsAreSortedAscOrDesc(newListAllLocations);
        serviceLocationsPage.clickOnSortingIcon();
        Collections.reverse(newListAllLocations);
        serviceLocationsPage.checkIfLocationsAreSortedAscOrDesc(newListAllLocations);
        serviceLocationsPage.clickOnSortingIcon();
        serviceLocationsPage.checkLocationsInListByAdminRoles(linkedLocationsToService, notLinkedLocationsToService);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6592")
    @Test
    public void checkSearchFunctionalityWithNonExistingService() {
        final Role role = getRandomRole();
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") : (role.equals(ADMIN) ? adminWithLocations.getString("token") : role.equals(LOCATION_ADMIN) ? locationAdminWithLocations.getString("token") : role.equals(STAFF) ? staffWithLocations.getString("token") : supportToken);
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, serviceId, userToken);
        serviceLocationsPage.openPage();
        final String stringToSearch = UUID.randomUUID().toString();
        serviceLocationsPage.checkLocationsCount(allLocations.size());
        serviceLocationsPage.searchForLocation(stringToSearch);
        serviceLocationsPage.checkLocationsCount(0);
        serviceLocationsPage.checkEmptySearchResult(stringToSearch);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6595")
    @Test
    public void checkSearchFunctionality() {
        final Role role = getRandomRole();
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") : role.equals(ADMIN) ? adminWithLocations.getString("token") : role.equals(LOCATION_ADMIN) ? locationAdminWithLocations.getString("token") : role.equals(STAFF) ? staffWithLocations.getString("token") : supportToken;
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, serviceId, userToken);
        serviceLocationsPage.openPage();
        final JSONObject searchedLocation = allLocations.get(2);
        final String searchedLocationName = searchedLocation.getString("internalName");
        serviceLocationsPage.checkLocationsCount(allLocations.size());
        serviceLocationsPage.searchForLocation(searchedLocationName);
        serviceLocationsPage.checkLocationsCount(1);
        serviceLocationsPage.checkSearchResult(0, searchedLocation);
        serviceLocationsPage.searchForLocation(searchedLocationName.substring(2, 8));
        serviceLocationsPage.checkLocationsCount(1);
        serviceLocationsPage.checkSearchResult(0, searchedLocation);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6606")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkLocationsToServiceByAdminRoles(Role role) {
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") : (role.equals(ADMIN) ? adminWithLocations.getString("token") : supportToken);
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, newServiceId, userToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.linkUnlinkLocationByIndex(2, ToggleAction.LINK);
        serviceLocationsPage.linkUnlinkLocationByIndex(4, ToggleAction.LINK);
        serviceLocationsPage.changeTab(Tabs.DETAILS);
        serviceLocationsPage.changeTab(Tabs.LOCATIONS);
        serviceLocationsPage.checkLocationsInListByAdminRoles(linkedLocationsToService, notLinkedLocationsToService);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6607")
    @Test
    public void linkLocationToServiceByLocationAdmin() {
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String locationAdminToken = locationAdminWithLocations.getString("token");
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, newServiceId, locationAdminToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.linkUnlinkLocationByIndex(0, ToggleAction.LINK);
        final List<JSONObject> locationsThatLinkedToService = List.of(allLocations.get(0));
        serviceLocationsPage.changeTab(Tabs.DETAILS);
        serviceLocationsPage.changeTab(Tabs.LOCATIONS);
        serviceLocationsPage.checkLocationsInListByLocationAdminRole(locationsLinkedToLocationAdmin, locationsThatLinkedToService);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6608")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void unlinkLocationToServiceByAdminRoles(Role role) {
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") : (role.equals(ADMIN) ? adminWithLocations.getString("token") : supportToken);
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, List.of(allLocations.get(0).getString("id"), allLocations.get(1).getString("id"), allLocations.get(2).getString("id")));
        final ServiceLocationsPage serviceLocationsPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, newServiceId, userToken);
        serviceLocationsPage.openPage();
        serviceLocationsPage.linkUnlinkLocationByIndex(0, ToggleAction.UNLINK);
        serviceLocationsPage.linkUnlinkLocationByIndex(1, ToggleAction.UNLINK);
        final List<JSONObject> locationsLinkedToNewService = List.of(allLocations.get(2));
        final List<JSONObject> locationsNotLinkedToNewService = List.of(allLocations.get(0), allLocations.get(1), allLocations.get(3), allLocations.get(4));
        serviceLocationsPage.changeTab(Tabs.DETAILS);
        serviceLocationsPage.changeTab(Tabs.LOCATIONS);
        serviceLocationsPage.checkLocationsInListByAdminRoles(locationsLinkedToNewService, locationsNotLinkedToNewService);
    }

    @Xray(requirement = "PEG-4792", test = "PEG-6609")
    @Test
    public void unlinkLocationToServiceByLocationAdmin() {
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, List.of(locationsLinkedToLocationAdmin.get(0).getString("id"), locationsLinkedToLocationAdmin.get(1).getString("id")));
        final String locationAdminToken = locationAdminWithLocations.getString("token");
        final ServiceLocationsPage serviceLocationPage = new ServiceLocationsPage(browserToUse, versionToBe, organizationId, newServiceId, locationAdminToken);
        serviceLocationPage.openPage();
        serviceLocationPage.linkUnlinkLocationByIndex(0, ToggleAction.UNLINK);
        final List<JSONObject> locationsThatLinkedToService = List.of(locationsLinkedToLocationAdmin.get(1));
        serviceLocationPage.changeTab(Tabs.DETAILS);
        serviceLocationPage.changeTab(Tabs.LOCATIONS);
        serviceLocationPage.checkLocationsInListByLocationAdminRole(locationsLinkedToLocationAdmin, locationsThatLinkedToService);
    }
}

