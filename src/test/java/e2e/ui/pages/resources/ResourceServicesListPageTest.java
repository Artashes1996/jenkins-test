package e2e.ui.pages.resources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.*;
import utils.*;
import utils.commons.ToggleAction;

import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;
import static utils.TestUtils.getRandomInt;

public class ResourceServicesListPageTest  extends BasePageTest {

    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private ServiceFlows serviceFlows;
    private ResourceFlows resourceFlows;

    private String supportToken;
    private JSONObject organizationAndUsers;
    private String organizationId;

    private List<JSONObject> locations;
    private List<String> locationIds;

    private List<JSONObject> services;
    private List<JSONObject> linkedServices;
    private List<JSONObject> notLinkedServices;

    private JSONObject resource;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        resourceFlows = new ResourceFlows();
        serviceFlows = new ServiceFlows();

        supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        locations = new ArrayList<>();

        final List<String> servicesIds = new ArrayList<>();
        linkedServices = new ArrayList<>();
        notLinkedServices = new ArrayList<>();

        locationIds = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            final JSONObject location = locationFlows.createLocation(organizationId);
            locations.add(location);
            locationIds.add(location.getString("id"));
            final JSONObject simpleService = serviceFlows.createService(organizationId);
            servicesIds.add(simpleService.getString("id"));
            if (i % 2 == 0) {
                notLinkedServices.add(simpleService);
            } else {
                linkedServices.add(simpleService);
            }
            final JSONObject hiddenService = serviceFlows.createHiddenService(organizationId);
            servicesIds.add(hiddenService.getString("id"));
            linkedServices.add(hiddenService);
            final JSONObject inactiveService = serviceFlows.createInactiveService(organizationId);
            servicesIds.add(inactiveService.getString("id"));
            linkedServices.add(inactiveService);
        }

        organizationAndUsers.put(ADMIN.name(), userFlows.createUser(organizationId, ADMIN, locationIds));
        organizationAndUsers.put(LOCATION_ADMIN.name(), userFlows.createUser(organizationId, LOCATION_ADMIN, locationIds));
        organizationAndUsers.put(STAFF.name(), userFlows.createUser(organizationId, STAFF, locationIds));
        userFlows.linkUnlinkLocationsToUser(organizationId, organizationAndUsers.getJSONObject(OWNER.name()).getString("id"),
                locationIds, ToggleAction.LINK);

        resource = resourceFlows.createActiveResource(organizationId, locationIds);

        for (String locationId : locationIds) {
            locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, servicesIds, ToggleAction.LINK);
            for (JSONObject service : linkedServices) {
                resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, resource.getString("id"), service.getString("id"), ToggleAction.LINK);
                userFlows.linkUnlinkUserToLocationService(organizationId, organizationAndUsers.getJSONObject(OWNER.name()).getString("id"),
                        locationId, service.getString("id"), ToggleAction.LINK);
                userFlows.linkUnlinkUserToLocationService(organizationId, organizationAndUsers.getJSONObject(ADMIN.name()).getString("id"),
                        locationId, service.getString("id"), ToggleAction.LINK);
                userFlows.linkUnlinkUserToLocationService(organizationId, organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"),
                        locationId, service.getString("id"), ToggleAction.LINK);
                userFlows.linkUnlinkUserToLocationService(organizationId, organizationAndUsers.getJSONObject(STAFF.name()).getString("id"),
                        locationId, service.getString("id"), ToggleAction.LINK);
            }
        }

        locations.sort(Comparator.comparing(location -> location.getString("internalName")));
        linkedServices.sort(Comparator.comparing(service -> service.getString("internalName")));
        notLinkedServices.sort(Comparator.comparing(service -> service.getString("internalName")));
        services = Stream.concat(linkedServices.stream(), notLinkedServices.stream()).collect(Collectors.toList());

    }

    @BeforeMethod(alwaysRun = true)
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6732")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class, retryAnalyzer = RetryAnalyzer.class)
    public void seeServicesListByInviters(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final ResourceServicesListPage resourceServicesListPage = role.equals(SUPPORT)
                ? new ResourceServicesListPage(browserToUse, versionToBe, organizationId, supportToken)
                : new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.checkLocation(locations.get(0));
        resourceServicesListPage.checkServicesInPageEditAccess(linkedServices, notLinkedServices);
        resourceServicesListPage.checkServicesCount(services);
    }

    @Xray(requirement = "PEG-5799", test = "PEG-6733")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void seeServicesListByStaff() {
        final String token = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.checkLocation(locations.get(0));
        resourceServicesListPage.checkServicesInPageViewAccess(linkedServices);
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6734")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkLocationDropdown() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        final String nonExistingLocation = UUID.randomUUID().toString();

        resourceServicesListPage.searchForLocation(locations.get(1).getString("internalName"));
        resourceServicesListPage.checkLocationsInDropdown(Collections.singletonList(locations.get(1)));
        resourceServicesListPage.searchForLocation(nonExistingLocation);
        resourceServicesListPage.checkEmptySearchResultInLocationDropdown(nonExistingLocation);
        resourceServicesListPage.clearLocationDropdownSearchField();
        resourceServicesListPage.checkLocation(locations.get(0));
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6735")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void searchForService() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        final JSONObject serviceToSearch = services.get(getRandomInt(services.size()));
        resourceServicesListPage.searchForService(serviceToSearch);
        resourceServicesListPage.checkService(0, serviceToSearch);
        resourceServicesListPage.checkServicesCount(Collections.singletonList(serviceToSearch));
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6736")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void userServicesListPagination() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject newLocation = locationFlows.createLocation(organizationId);
        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocation.getString("id")));

        final List<String> newLocationServicesIds = new ArrayList<>();
        final List<JSONObject> newLocationServices = new ArrayList<>();

        IntStream.range(0, 51).forEach(index -> {
            final JSONObject service = serviceFlows.createService(organizationId);
            newLocationServices.add(service);
            newLocationServicesIds.add(service.getString("id"));
        });
        newLocationServices.sort(Comparator.comparing(service -> service.getString("internalName")));
        locationFlows.linkUnlinkServicesToLocation(organizationId, newLocation.getString("id"),
                newLocationServicesIds, ToggleAction.LINK);

        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.checkLocation(newLocation);
        resourceServicesListPage.checkPagination(newLocationServices);
    }

    @Xray(requirement = "PEG-5799", test = "PEG-6737")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void seeServicesListByLocationAdminNotLinked() {
        final JSONObject notLinkedLocationAdmin = userFlows.createUser(organizationId, LOCATION_ADMIN,
                Collections.singletonList(locations.get(0).getString("id")));
        final String token = notLinkedLocationAdmin.getString("token");
        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.selectLocation(locations.get(1));
        resourceServicesListPage.checkServicesInPageViewAccess(linkedServices);
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6738")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void searchServiceEmptyResult() {
        final String token = organizationAndUsers.getJSONObject(getRandomOrganizationInviterRole().name())
                .getString("token");
        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.checkSearchEmptyResult();
    }

    @Xray(requirement = "PEG-5800", test = "PEG-6739")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkLocationServiceToResource(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject resource = resourceFlows.createActiveResource(organizationId, locationIds);
        final JSONObject targetLocation = locations.get(getRandomInt(locations.size()));

        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.selectLocation(targetLocation);

        final int serviceRow = getRandomInt(services.size());
        resourceServicesListPage.linkUnlinkLocationServiceToUser(serviceRow, ToggleAction.LINK);
        resourceServicesListPage.linkUnlinkLocationServiceToUser(serviceRow, ToggleAction.UNLINK);
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6768")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkOrderOfServicedAfterLinkUnlink() {
        final JSONObject targetLocation = locationFlows.createLocation(organizationId); //locations.get(getRandomInt(locations.size()));
        final List<JSONObject> servicesLinkedToTargetLocation = Arrays.asList(services.get(0), services.get(1));
        final List<String> serviceIdsLinkedToTargetLocation = servicesLinkedToTargetLocation.stream().map((JSONObject obj) -> obj.getString("id")).collect(Collectors.toList());
        locationFlows.linkUnlinkServicesToLocation(organizationId, targetLocation.getString("id"), serviceIdsLinkedToTargetLocation, ToggleAction.LINK);
        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(targetLocation.getString("id")));

        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        resourceServicesListPage.resourceToEnter.set(resource);
        resourceServicesListPage.openPage();
        resourceServicesListPage.linkUnlinkLocationServiceToUser(1, ToggleAction.LINK);
        resourceServicesListPage.refreshPage();

        resourceServicesListPage.checkServicesInPageEditAccess(Collections.singletonList(services.get(1)), Collections.singletonList(services.get(0)));
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6740")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void servicePageOfResourceWithNoLinkedLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final ResourceServicesListPage resourceServicesListPage = new ResourceServicesListPage(browserToUse, versionToBe, token);
        resourceServicesListPage.resourceToEnter.set(resourceFlows.createActiveResource(organizationId, null));
        resourceServicesListPage.openPage();
        resourceServicesListPage.checkNoLocation();
    }

    @Xray(requirement = "PEG-5561", test = "PEG-6741")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void servicePageOfResourceWithOneLinkedLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        final JSONObject linkedOnlyLocation = locations.get(0);
        userServicesListPage.userToEnter.set(userFlows.createUser(organizationId, getRandomOrganizationAdminRole(),
                Collections.singletonList(linkedOnlyLocation.getString("id"))));
        userServicesListPage.openPage();
        userServicesListPage.checkSingleLinkedLocation(linkedOnlyLocation);
    }

}
