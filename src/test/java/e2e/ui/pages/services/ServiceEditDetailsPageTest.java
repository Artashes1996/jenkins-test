package e2e.ui.pages.services;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import helpers.flows.*;
import org.json.*;
import org.testng.annotations.*;
import pages.*;
import utils.*;
import java.util.Collections;

import static configuration.Role.*;
import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.*;
import static org.testng.Assert.*;

public class ServiceEditDetailsPageTest extends BasePageTest {
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

    @Xray(requirement = "PEG-4599", test = "7064")
    @Test
    public void checkDurationError(){
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.checkDurationError();
    }

    @Xray(requirement = "PEG-4599", test = "7063")
    @Test
    public void checkDurationRequiredFieldError(){
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.checkDurationRequiredFieldError();
    }

    @Xray(requirement = "PEG-4599", test = "7062")
    @Test
    public void changeAllSettings(){
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String serviceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, serviceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, serviceId, token);
        serviceDetailsEditModePage.openPage();

        final JSONObject updatedService = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateCombination.ALL_FIELDS);
        serviceDetailsEditModePage.changeAllSettings(updatedService);

        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, service.getString("id"));
        serviceDetailsViewModePage.checkToast();

        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkServiceDetails(service);
    }

    @Xray(requirement = "PEG-4599", test = "7061")
    @Test
    public void checkAllSettings(){
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.checkAllSettings(service);

        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, service.getString("id"));

        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkServiceDetails(service);
    }

    @Xray(requirement = "PEG-4599", test = "7060")
    @Test
    public void changeVisibility() {
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createHiddenService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.changeVisibility(service);
        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, service.getString("id"));
        serviceDetailsViewModePage.checkToast();

        service.getJSONObject(VISIBILITY).put(MONITOR, true);

        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkServiceDetails(service);

    }

    @Xray(requirement = "PEG-4599", test = "7059")
    @Test
    public void checkVisibility() {
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.checkVisibility(service);
        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, service.getString("id"));

        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkServiceDetails(service);

    }

    @Xray(requirement = "PEG-4599", test = "7058")
    @Test
    public void checkFields() {
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject randomField = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        final JSONObject fields = serviceFlows.createServiceWithFields(organizationId, Collections.singletonList(randomField));
        final String newServiceId = fields.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.checkFields(fields);

        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, fields.getString("id"));
        serviceDetailsViewModePage.checkToast();

        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkServiceDetails(fields);

    }

    @Xray(requirement = "PEG-4599", test = "7057")
    @Test
    public void changeFields() {
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        final JSONArray fieldsToAdd = fieldsFlows.getDefaultFields(organizationId);
        serviceDetailsEditModePage.addFieldFromPopup(fieldsToAdd);
        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, service.getString("id"));
        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkToast();
        final JSONObject serviceAfterUpdate = serviceFlows.getServiceById(organizationId, service.getString("id"));
        assertFalse(service.similar(serviceAfterUpdate));
        serviceDetailsViewModePage.checkServiceDetails(serviceAfterUpdate);
    }

    @Xray(requirement = "PEG-4599", test = "7055")
    @Test
    public void checkCancelChanges() {
        final Role role = getRandomAdminRole();

        final String token = role.equals(SUPPORT) ? supportToken :
                organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject service = serviceFlows.createService(organizationId);
        final String newServiceId = service.getString("id");
        final ServiceDetailsEditModePage serviceDetailsEditModePage = role.equals(Role.SUPPORT) ?
                new ServiceDetailsEditModePage(browserToUse, versionToBe, organizationId, newServiceId, supportToken) :
                new ServiceDetailsEditModePage(browserToUse, versionToBe, newServiceId, token);
        serviceDetailsEditModePage.openPage();
        serviceDetailsEditModePage.cancelChanges();

        final ServiceDetailsViewModePage serviceDetailsViewModePage = new ServiceDetailsViewModePage(browserToUse, versionToBe, service.getString("id"));

        serviceDetailsViewModePage.isLoaded();
        serviceDetailsViewModePage.checkServiceDetails(service);
    }


}
