package e2e.ui.pages.services;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.FieldsFlows;
import helpers.flows.OrganizationFlows;

import helpers.flows.ServiceFlows;
import org.json.*;
import org.testng.annotations.*;
import pages.CreateServicePage;
import pages.ServicesListPage;
import pages.SignInPage;
import utils.Xray;

import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;

public class CreateServicePageTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private JSONObject field;
    private String organizationId;
    private List<JSONObject> serviceDefaultFields;
    private List<JSONObject> notServiceDefaultFields;

    @BeforeClass
    public void setUp() {
        final FieldsFlows fieldsFlows = new FieldsFlows();
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
        supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
        final JSONArray serviceDefaultFieldsArray = fieldsFlows.getServiceDefaultFields(organizationId);
        final JSONArray defaultFieldsArray = fieldsFlows.getDefaultFields(organizationId);
        serviceDefaultFields = IntStream.range(0, serviceDefaultFieldsArray.length()).
                mapToObj(serviceDefaultFieldsArray::getJSONObject).collect(Collectors.toList());
        final List<JSONObject> defaultFields = IntStream.range(0, defaultFieldsArray.length()).
                mapToObj(defaultFieldsArray::getJSONObject).collect(Collectors.toList());
        field = new FieldsFlows().createField(organizationId, FieldTypes.SINGLE_SELECT_DROPDOWN);
        notServiceDefaultFields = Stream.concat(defaultFields.stream(), serviceDefaultFields.stream()).collect(Collectors.toList());
        notServiceDefaultFields
                .removeIf(defaultField -> {
                    for (final JSONObject serviceDefaultField :
                            serviceDefaultFields) {
                        if (defaultField.getString("internalName").equals(serviceDefaultField.getString("internalName"))) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5422")
    @Test
    public void checkRequiredValidations() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clearServiceDurationValue();
        createServicePage.clickOnCreateButton();
        createServicePage.checkRequiredErrorMessageForServiceNameField();
        createServicePage.checkRequiredErrorMessageForDisplayNameField();
        createServicePage.checkRequiredErrorMessageForServiceDurationField();
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5424")
    @Test
    public void checkUniqueServiceNameForOrganization() {
        final ServiceFlows serviceFlows = new ServiceFlows();
        final String serviceName = serviceFlows.createService(organizationId).getString("internalName");
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.fillValueInServiceNameField(serviceName);
        createServicePage.clickOnCreateButton();
        createServicePage.checkExistingServiceErrorMsg();
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5429")
    @Test
    public void serviceDurationCannotBeLessThanFiveMinutes() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.fillValueInServiceDurationField("4");
        createServicePage.clickOnCreateButton();
        createServicePage.checkServiceDurationCannotBeLessThanFiveMinutes();
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5423")
    @Test
    public void checkCancelButtonFunctionality() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnVisibilityToggle();
        createServicePage.clickOnCancelButton();
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe);
        servicesListPage.isLoaded();
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5425")
    @Test
    public void displayNameIsAutoFilled() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final String serviceName = UUID.randomUUID() + "service_name";
        createServicePage.fillValueInServiceNameField(serviceName);
        createServicePage.clickOnStatusToggle();
        createServicePage.checkDisplayNameFieldValue(serviceName);
        createServicePage.clearDisplayNameFieldValue();
        createServicePage.clickOnStatusToggle();
        createServicePage.checkDisplayNameFieldValue(serviceName);
        createServicePage.clearServiceNameFieldValue();
        createServicePage.checkDisplayNameFieldValue(serviceName);
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5427")
    @Test
    public void checkVisibilityOffOn() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.checkVisibilityState(CreateServicePage.Visibility.VISIBILITY_ON);
        createServicePage.clickOnVisibilityToggle();
        createServicePage.checkCheckBoxesNotDisplayed();
        createServicePage.clickOnVisibilityToggle();
        createServicePage.checkCheckboxesAreSelected();
        createServicePage.checkWebKioskCheckbox();
        createServicePage.checkPhysicalKioskCheckbox();
        createServicePage.checkMonitorCheckbox();
        createServicePage.checkVisibilityState(CreateServicePage.Visibility.HIDDEN);
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5519")
    @Test
    public void checkThatTextNotAcceptedDurationField() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.fillValueInServiceDurationField("abcd");
        createServicePage.checkServiceDurationValue("5");
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5426")
    @Test
    public void checkByDefaultState() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.checkSelectedResourceSelection("Disabled");
        createServicePage.checkStatusState("Active");
        createServicePage.checkServiceDurationValue("5");
        createServicePage.checkVisibilityState(CreateServicePage.Visibility.VISIBILITY_ON);
        createServicePage.checkCheckboxesAreSelected();
    }

    @Xray(requirement = "PEG-3672", test = "PEG-5436")
    @Test()
    public void createActiveService() {
        final Role role = getRandomAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final String serviceName = UUID.randomUUID() + " service_name";
        final String serviceDisplayName = UUID.randomUUID() + " service_name";
        createServicePage.fillServiceFields(serviceName, serviceDisplayName, "8", CreateServicePage.ResourceSelectionType.ALLOWED);
        createServicePage.checkMonitorCheckbox();
        createServicePage.checkWebKioskCheckbox();
        createServicePage.clickOnCreateButton();
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe);
        servicesListPage.searchServices(serviceName);
        servicesListPage.checkServiceData(serviceName + serviceDisplayName, "Visible", "Active", "8 Minutes");
    }


    @Xray(requirement = "PEG-3672", test = "PEG-5458")
    @Test
    public void createServiceWithInactiveStatusAndVisibilityIsHidden() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final String serviceName = UUID.randomUUID() + " service_name";
        final String serviceDisplayName = UUID.randomUUID() + " service_name";
        createServicePage.fillServiceFields(serviceName, serviceDisplayName, "8", CreateServicePage.ResourceSelectionType.DISABLED);
        createServicePage.clickOnStatusToggle();
        createServicePage.clickOnVisibilityToggle();
        createServicePage.clickOnCreateButton();
        final ServicesListPage servicesListPage = new ServicesListPage(browserToUse, versionToBe);
        servicesListPage.openPage();
        servicesListPage.searchServices(serviceName);
        servicesListPage.checkServiceData(serviceName + serviceDisplayName, "Hidden", "Inactive", "8 Minutes");
    }

    @Xray(requirement = "PEG-4611", test = "PEG-6758")
    @Test
    public void seeDefaultFields() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final JSONArray serviceDefaultFields = new JSONArray(this.serviceDefaultFields.toArray());
        createServicePage.checkFieldsData(serviceDefaultFields);
    }

    @Xray(requirement = "PEG-4611", test = "PEG-6761")
    @Test
    public void checkDeleteAction() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.deleteAllFields();
        createServicePage.checkEmptyResult();
    }

    @Xray(requirement = "PEG-4611", test = "PEG-6828")
    @Test
    public void createServiceWithFieldConfigurations() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken) :
                new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final String serviceName = UUID.randomUUID() + " service_name";
        createServicePage.fillValueInServiceNameField(serviceName);
        createServicePage.checkHiddenOnKiosk(0);
        createServicePage.checkRequired(1);
        createServicePage.clickOnCreateButton();
        createServicePage.checkSuccessToast();
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6813")
    @Test
    public void seeNotSelectedFields() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.checkAddFieldsButtonIsDisabled();
        notServiceDefaultFields.add(field);
        createServicePage.checkFieldsDataInAddFieldPopup(notServiceDefaultFields);
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6814")
    @Test
    public void searchField() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.searchField(field.getString("internalName"));
        createServicePage.checkFieldDataInAddFieldsPopup(0, field);
    }


    @Xray(requirement = "PEG-5888", test = "PEG-6815")
    @Test
    public void searchNotValidField() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken) : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.checkEmptySearchResult();
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6816")
    @Test
    public void addFieldsPopupEmptyState() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.addAllFieldsExistingInPopup();
        createServicePage.clickOnAddFieldButton();
        createServicePage.checkEmptyResult();
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6817")
    @Test
    public void addFieldsFromAddFieldPopup() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.selectFieldFromAddFieldPopup(1);
        createServicePage.selectFieldFromAddFieldPopup(2);
        createServicePage.clickOnAddFieldsButtonFromPopup();
        final JSONArray selectedFields = new JSONArray(serviceDefaultFields.toArray());
        selectedFields.put(notServiceDefaultFields.get(1)).put(notServiceDefaultFields.get(2));
        createServicePage.checkFieldsData(selectedFields);
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6819")
    @Test
    public void addedFieldsNotDisplayedInAddFieldsPopup() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.selectFieldFromAddFieldPopup(1);
        createServicePage.clickOnAddFieldsButtonFromPopup();
        createServicePage.clickOnAddFieldButton();
        createServicePage.checkFieldNotPresentInPopup(notServiceDefaultFields.get(1));
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6820")
    @Test
    public void addFields() {
        final Role role = getRandomOrganizationAdminRole();
        final JSONArray selectedFields = new JSONArray(serviceDefaultFields.toArray());
        selectedFields.put(notServiceDefaultFields.get(1));
        selectedFields.put(field);
        final CreateServicePage createServicePage = role.equals(SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken) :
                new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.searchField(notServiceDefaultFields.get(1).getString("internalName"));
        createServicePage.selectFieldFromAddFieldPopup(0);
        createServicePage.searchField(field.getString("internalName"));
        createServicePage.selectFieldFromAddFieldPopup(0);
        createServicePage.checkAddFieldsButtonText(2);
        createServicePage.clickOnAddFieldsButtonFromPopup();
        createServicePage.checkFieldsData(selectedFields);
    }

    @Xray(requirement = "PEG-5888", test = "PEG-6821")
    @Test
    public void closePopupByCancel() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        createServicePage.clickOnAddFieldButton();
        createServicePage.clickOnCancelButtonFromFieldsPopup();
        createServicePage.checkFieldsPopupIsNotDisplayed();
    }

    @Xray(requirement = "PEG-5150", test = "PEG-7163")
    @Test
    public void arrowUpAndDownButtonsDisabled() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final int indexOfFirstField = 0;
        final int indexOfLastField = 3;
        createServicePage.arrowUpButtonIsDisabled(indexOfFirstField);
        createServicePage.arrowDownButtonIsDisabled(indexOfLastField);
        createServicePage.deleteField(indexOfFirstField);
        createServicePage.deleteField(indexOfFirstField);
        createServicePage.deleteField(indexOfFirstField);
        createServicePage.arrowDownButtonIsDisabled(indexOfFirstField);
        createServicePage.arrowDownButtonIsDisabled(indexOfFirstField);
    }

    @Xray(requirement = "PEG-5150", test = "PEG-7164")
    @Test
    public void sortFields() {
        final Role role = getRandomOrganizationAdminRole();
        final CreateServicePage createServicePage = role.equals(Role.SUPPORT) ? new CreateServicePage(browserToUse, versionToBe, organizationId, supportToken)
                : new CreateServicePage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        createServicePage.openPage();
        final int indexOfFirstField = 0;
        final int indexOfSecondField = 1;
        final int indexOfThirdField = 2;
        final int indexOfLastField = 3;
        createServicePage.moveUp(indexOfSecondField);
        createServicePage.moveDown(indexOfThirdField);
        final List<JSONObject> copyOfServiceDefaultFields = new ArrayList<>(serviceDefaultFields);
        Collections.swap(copyOfServiceDefaultFields, indexOfFirstField, indexOfSecondField);
        Collections.swap(copyOfServiceDefaultFields, indexOfThirdField, indexOfLastField);
        final JSONArray fields = new JSONArray();
        IntStream.range(0, copyOfServiceDefaultFields.size()).forEach(i -> {
            fields.put(copyOfServiceDefaultFields.get(i));
        });
        createServicePage.checkFieldsData(fields);
    }

}

