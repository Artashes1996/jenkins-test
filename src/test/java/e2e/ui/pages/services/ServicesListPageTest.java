package e2e.ui.pages.services;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.ServiceFlows;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.ServicesListPage;
import pages.SignInPage;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static configuration.Role.*;
import static utils.TestUtils.*;

public class ServicesListPageTest extends BasePageTest {

    private JSONObject organizationAndUsers;
    private List<JSONObject> services;
    private List<String> servicesNames;
    private String supportToken;
    private String organizationId;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final ServiceFlows serviceFlows = new ServiceFlows();
        supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        services = new ArrayList<>();
        servicesNames = new ArrayList<>();
        IntStream.range(0, 51).forEach(i -> {
            if (i % 3 == 0) {
                services.add(serviceFlows.createService(organizationId));
            } else if (i % 3 == 1) {
                services.add(serviceFlows.createHiddenService(organizationId));
            } else {
                services.add(serviceFlows.createInactiveService(organizationId));
            }
        });

        services = services.stream().sorted(Comparator.comparing((JSONObject singleService) -> singleService.getString("internalName")))
                .collect(Collectors.toList());

        services.forEach(service -> servicesNames.add(service.getString("internalName")));

    }

    @BeforeMethod(alwaysRun = true)
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(test = "PEG-5477", requirement = "PEG-3839")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void seeServicesListByAdminRoles(Role role) {
        ServicesListPage servicesListPage = null;
        if(role.equals(SUPPORT)){
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        }else{
            final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, token);
        }

        servicesListPage.openPage();
        final int indexToCheck = getRandomInt(50);
        servicesListPage.checkActionColumnIsPresent();
        servicesListPage.checkCreateServiceButtonIsDisplayed();
        servicesListPage.checkServiceByIndex(indexToCheck, services.get(indexToCheck));
    }

    @Xray(test = "PEG-5478", requirement = "PEG-3839")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void seeServicesListByLocationUsers(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe, token);
        servicesListPage.openPage();
        final int indexToCheck = getRandomInt(50);
        servicesListPage.checkServiceByIndex(indexToCheck, services.get(indexToCheck));
        servicesListPage.checkActionColumnIsNotPresent();
        servicesListPage.checkCreateServiceButtonIsNotDisplayed();
    }

    @Xray(test = "PEG-5482", requirement = "PEG-3839")
    @Test
    public void servicesListSorting() {
        final Role randomRole = getRandomRole();
        ServicesListPage servicesListPage = null;
        if(randomRole.equals(SUPPORT)){
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        }else{
            final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, token);
        }

        servicesListPage.openPage();
        servicesListPage.checkSorting(servicesNames);
        servicesListPage.changeSorting();
        final List<String> servicesNamesDesc = servicesNames.subList(0, servicesNames.size());
        Collections.reverse(servicesNamesDesc);
        servicesListPage.checkSorting(servicesNamesDesc);
    }

    @Xray(test = "PEG-5480", requirement = "PEG-3839")
    @Test
    public void searchServices() {
        final Role randomRole = getRandomRole();
        ServicesListPage servicesListPage = null;
        if(randomRole.equals(SUPPORT)){
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        }else{
            final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, token);
        }
        servicesListPage.openPage();
        final JSONObject serviceToSearch = services.get(getRandomInt(services.size()));
        servicesListPage.searchServices(serviceToSearch.getString("internalName"));
        servicesListPage.checkServiceByIndex(0, serviceToSearch);
    }

    @Xray(test = "PEG-5479", requirement = "PEG-3839")
    @Test
    public void servicesPagination() {
        final Role randomRole = getRandomRole();
        ServicesListPage servicesListPage = null;
        if(randomRole.equals(SUPPORT)){
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        }else{
            final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
            servicesListPage = new ServicesListPage(browserToUse, versionToBe, token);
        }

        servicesListPage.openPage();
        servicesListPage.checkPaginationNumbers(services);
        servicesListPage.checkPagination(services);
    }

    @Xray(test = "PEG-5497", requirement = "PEG-3839")
    @Test
    public void checkEmptyPage() {
        final JSONObject organizationWithNoService = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final String ownerToken = organizationWithNoService.getJSONObject(OWNER.name()).getString("token");
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe, ownerToken);
        servicesListPage.openPage();
        servicesListPage.checkEmptyServiceList();
    }

    @Xray(test = "PEG-5496", requirement = "PEG-3839")
    @Test
    public void searchEmptyResult() {
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe, organizationId, supportToken);
        servicesListPage.openPage();
        servicesListPage.checkSearchNoResult();
    }

}
