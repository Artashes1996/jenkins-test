package e2e.ui.pages.users;

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
import static utils.TestUtils.*;

public class UserServicesListPageTest extends BasePageTest {

    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private ServiceFlows serviceFlows;

    private String supportToken;
    private JSONObject organizationAndUsers;
    private String organizationId;

    private List<JSONObject> locations;
    private List<String> locationIds;

    private List<JSONObject> services;
    private List<JSONObject> linkedServices;
    private List<JSONObject> notLinkedServices;

    @BeforeMethod(alwaysRun = true)
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();

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

        for (String locationId : locationIds) {
            locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, servicesIds, ToggleAction.LINK);
            for (JSONObject service : linkedServices) {
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

    @Xray(test = "PEG-6051", requirement = "PEG-4953")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void seeServicesListByInviters(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final UserServicesListPage userServicesListPage = role.equals(SUPPORT)
                ? new UserServicesListPage(browserToUse, versionToBe, organizationId, supportToken)
                : new UserServicesListPage(browserToUse, versionToBe, token);
        final JSONObject user = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name());
        userServicesListPage.userToEnter.set(user);
        userServicesListPage.openPage();
        userServicesListPage.checkUserDetails(user);
        userServicesListPage.checkLocation(locations.get(0));
        userServicesListPage.checkServicesInPageEditAccess(linkedServices, notLinkedServices);
        userServicesListPage.checkServicesCount(services);
    }

    @Xray(test = "PEG-6054", requirement = "PEG-4953")
    @Test
    public void seeServicesListByStaff() {
        final String token = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        final JSONObject user = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name());
        userServicesListPage.userToEnter.set(user);
        userServicesListPage.openPage();
        userServicesListPage.checkUserDetails(user);
        userServicesListPage.checkLocation(locations.get(0));
        userServicesListPage.checkServicesInPageViewAccess(linkedServices);
    }

    @Xray(test = "PEG-6055", requirement = "PEG-4953")
    @Test
    public void checkLocationDropdown() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        final JSONObject user = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name());
        userServicesListPage.userToEnter.set(user);
        userServicesListPage.openPage();
        final String nonExistingLocation = UUID.randomUUID().toString();

        userServicesListPage.searchForLocation(locations.get(1).getString("internalName"));
        userServicesListPage.checkLocationsInDropdown(Collections.singletonList(locations.get(1)));
        userServicesListPage.searchForLocation(nonExistingLocation);
        userServicesListPage.checkEmptySearchResultInLocationDropdown(nonExistingLocation);
        userServicesListPage.clearLocationDropdownSearchField();
        userServicesListPage.checkLocation(locations.get(0));
    }

    @Xray(test = "PEG-6056", requirement = "PEG-4953")
    @Test
    public void searchForService() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        final JSONObject user = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name());
        userServicesListPage.userToEnter.set(user);
        userServicesListPage.openPage();
        final JSONObject serviceToSearch = services.get(getRandomInt(services.size()));
        userServicesListPage.searchForService(serviceToSearch);
        userServicesListPage.checkService(0, serviceToSearch);
        userServicesListPage.checkServicesCount(Collections.singletonList(serviceToSearch));
    }

    @Xray(test = "PEG-6057", requirement = "PEG-4953")
    @Test
    public void userServicesListPagination() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject user = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name());
        final JSONObject newLocation = locationFlows.createLocation(organizationId);

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
        userFlows.linkUnlinkLocationToUser(organizationId, user.getString("id"), newLocation.getString("id"), ToggleAction.LINK);

        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        userServicesListPage.userToEnter.set(user);
        userServicesListPage.openPage();
        userServicesListPage.selectLocation(newLocation);
        userServicesListPage.checkLocation(newLocation);
        userServicesListPage.checkPagination(newLocationServices);
    }

    @Xray(test = "PEG-6053", requirement = "PEG-4953")
    @Test
    public void seeServicesListByLocationAdminNotLinked() {
        final JSONObject notLinkedLocationAdmin = userFlows.createUser(organizationId, LOCATION_ADMIN,
                Collections.singletonList(locations.get(0).getString("id")));
        final Role randomRole = getRandomOrganizationRole();
        final String token = notLinkedLocationAdmin.getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        userServicesListPage.userToEnter.set(organizationAndUsers.getJSONObject(randomRole.name()));
        userServicesListPage.openPage();
        userServicesListPage.selectLocation(locations.get(1));
        userServicesListPage.checkServicesInPageViewAccess(linkedServices);
    }

    @Xray(test = "PEG-6058", requirement = "PEG-4953")
    @Test
    public void searchServiceEmptyResult() {
        final String token = organizationAndUsers.getJSONObject(getRandomOrganizationInviterRole().name())
                .getString("token");
        final Role randomRole = getRandomOrganizationRole();
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        userServicesListPage.userToEnter.set(organizationAndUsers.getJSONObject(randomRole.name()));
        userServicesListPage.openPage();
        userServicesListPage.checkSearchEmptyResult();
    }

    @Xray(test = "PEG-6052", requirement = "PEG-4953")
    @Test
    public void seeLocationWithNoServices() {
        final JSONObject noServiceLocation = locationFlows.createLocation(organizationId);
        final JSONObject randomUserToEnter = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name());
        userFlows.linkUnlinkLocationToUser(organizationId, randomUserToEnter.getString("id"),
                noServiceLocation.getString("id"), ToggleAction.LINK);
        final String token = organizationAndUsers.getJSONObject(getRandomOrganizationRole().name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        userServicesListPage.userToEnter.set(randomUserToEnter);
        userServicesListPage.openPage();
        userServicesListPage.selectLocation(noServiceLocation);
        userServicesListPage.checkNoServicesPage();
    }

    @Xray(test = "PEG-6061", requirement = "PEG-4971")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkLocationServiceToUser(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject targetUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), locationIds);
        final JSONObject targetLocation = locations.get(getRandomInt(locations.size()));

        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        userServicesListPage.userToEnter.set(targetUser);
        userServicesListPage.openPage();
        userServicesListPage.selectLocation(targetLocation);

        final int serviceRow = getRandomInt(services.size());
        userServicesListPage.linkUnlinkLocationServiceToUser(serviceRow, ToggleAction.LINK);
        userServicesListPage.linkUnlinkLocationServiceToUser(serviceRow, ToggleAction.UNLINK);
    }

    @Xray(test = "PEG-6387", requirement = "PEG-4953")
    @Test
    public void servicePageOfUserWithNoLinkedLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        userServicesListPage.userToEnter.set(userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), null));
        userServicesListPage.openPage();
        userServicesListPage.checkNoLocation();
    }

    @Xray(test = "PEG-6388", requirement = "PEG-4953")
    @Test
    public void servicePageOfUserWithOneLinkedLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, token);
        final JSONObject linkedOnlyLocation = locations.get(0);
        userServicesListPage.userToEnter.set(userFlows.createUser(organizationId, getRandomOrganizationAdminRole(),
                Collections.singletonList(linkedOnlyLocation.getString("id"))));
        userServicesListPage.openPage();
        userServicesListPage.checkSingleLinkedLocation(linkedOnlyLocation);
    }

    @Xray(requirement = "PEG-4953", test = "PEG-6780")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkOrderOfServicedAfterLinkUnlink() {
        final JSONObject targetLocation = locationFlows.createLocation(organizationId); //locations.get(getRandomInt(locations.size()));
        final List<JSONObject> servicesLinkedToTargetLocation = Arrays.asList(services.get(0), services.get(1));
        final List<String> serviceIdsLinkedToTargetLocation = servicesLinkedToTargetLocation.stream().map((JSONObject obj) -> obj.getString("id")).collect(Collectors.toList());
        locationFlows.linkUnlinkServicesToLocation(organizationId, targetLocation.getString("id"), serviceIdsLinkedToTargetLocation, ToggleAction.LINK);
        final JSONObject user = userFlows.createUser(organizationId, STAFF,Collections.singletonList(targetLocation.getString("id")));

        final  UserServicesListPage userServicesListPage = new UserServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        userServicesListPage.userToEnter.set(user);
        userServicesListPage.openPage();
        userServicesListPage.linkUnlinkLocationServiceToUser(1, ToggleAction.LINK);
        userServicesListPage.refreshPage();

        userServicesListPage.checkServicesInPageEditAccess(Collections.singletonList(services.get(1)), Collections.singletonList(services.get(0)));
    }

}
