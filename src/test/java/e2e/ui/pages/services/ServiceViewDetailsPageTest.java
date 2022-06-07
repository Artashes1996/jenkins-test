package e2e.ui.pages.services;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.*;
import utils.*;
import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.*;


public class ServiceViewDetailsPageTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private String organizationId;
    private ServiceFlows serviceFlows;
    private FieldsFlows fieldsFlows;

    @BeforeClass
    public void setUp() {
        fieldsFlows = new FieldsFlows();
        serviceFlows = new ServiceFlows();
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
        supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3868", test = "PEG-5371")
    @Test
    public void seeServiceView() {
        final Role role = getRandomAdminRole();
        final JSONObject field = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        field.put(DISPLAY_TO, TestUtils.getRandomElementFromList(Arrays.asList(ServiceUpdateRequestBody.DisplayTo.values())).name());
        field.put(OPTIONAL, Math.random() < 0.5);
        field.put(ORDER, 0);
        final JSONObject serviceObject = serviceFlows.createServiceWithFields(organizationId, Collections.singletonList(field));
        final String newServiceId = serviceObject.getString("id");
        final ServiceDetailsViewModePage serviceDetailsViewModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsViewModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsViewModePage(browserToUse, versionToBe, newServiceId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        serviceDetailsViewModePage.openPage();
        serviceDetailsViewModePage.checkServiceDetails(serviceObject);
    }

    @Xray(requirement = "PEG-3868", test = "PEG-6904")
    @Test
    public void seeServiceViewWithoutFields() {
        final Role role = getRandomAdminRole();

        final JSONObject serviceObject = serviceFlows.createService(organizationId);
        final String newServiceId = serviceObject.getString("id");
        final ServiceDetailsViewModePage serviceDetailsViewModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsViewModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsViewModePage(browserToUse, versionToBe, newServiceId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        serviceDetailsViewModePage.openPage();
        serviceDetailsViewModePage.checkServiceDetails(serviceObject);
    }

    @Xray(requirement = "PEG-3868", test = "PEG-5374")
    @Test
    public void seeServiceInCaseOfInactiveStatus() {
        final Role role = getRandomAdminRole();
        final JSONObject inactiveServiceId = serviceFlows.createInactiveService(organizationId);
        final String newServiceId = inactiveServiceId.getString("id");
        final ServiceDetailsViewModePage serviceDetailsViewModePage = role.equals(Role.SUPPORT) ? new ServiceDetailsViewModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsViewModePage(browserToUse, versionToBe, newServiceId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        serviceDetailsViewModePage.openPage();
        serviceDetailsViewModePage.checkServiceDetails(inactiveServiceId);
    }

    @Xray(requirement = "PEG-3868", test = "PEG-5382")
    @Test
    public void seeServiceInCaseOfHiddenService() {
        final Role role = getRandomAdminRole();
        final JSONObject hiddenServiceId = serviceFlows.createHiddenService(organizationId);
        final String newServiceId = hiddenServiceId.getString("id");
        final ServiceDetailsViewModePage serviceDetailsViewModePage = role.equals(Role.SUPPORT) ? new ServiceDetailsViewModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsViewModePage(browserToUse, versionToBe, newServiceId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        serviceDetailsViewModePage.openPage();
        serviceDetailsViewModePage.checkServiceDetails(hiddenServiceId);
    }

    @Xray(requirement = "PEG-3868", test = "PEG-5384")
    @Test
    public void serviceViewInCaseOfUnspecifiedDisplayName() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken) :
                new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final String serviceName = UUID.randomUUID() + " service_name";
        createServicePage.fillValueInServiceNameField(serviceName);
        createServicePage.clickOnCreateButton();
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe);
        servicesListPage.searchServices(serviceName);
        servicesListPage.clickOnFirstServiceName();
        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, organizationId, supportToken);
        serviceDetailsViewModePage.checkDisplayNameFieldValue(serviceName);
    }

    @Xray(test = "PEG-5611", requirement = "PEG-4524")
    @Test
    public void breadCrumbViewAndFunctionality() {
        final JSONObject service = serviceFlows.createService(organizationId);
        final String serviceId = service.getString("id");
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken
                : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final ServiceDetailsViewModePage serviceDetailsViewModePage = role.equals(Role.SUPPORT)
                ? new ServiceDetailsViewModePage(browserToUse, versionToBe, organizationId, serviceId, token) :
                new ServiceDetailsViewModePage(browserToUse, versionToBe, serviceId, token);
        serviceDetailsViewModePage.openPage();
        serviceDetailsViewModePage.checkBreadcrumb(service);
    }
}

