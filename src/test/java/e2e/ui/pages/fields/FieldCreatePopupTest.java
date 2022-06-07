package e2e.ui.pages.fields;

import configuration.Role;
import e2e.gatewayapps.fieldsresource.data.FieldsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.fieldsresource.payloads.FieldsCreationBody;
import helpers.flows.OrganizationFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.FieldsListPage;
import pages.SignInPage;
import utils.Xray;

import static helpers.appsapi.fieldsresource.payloads.FieldsCreationBody.*;
import static utils.TestUtils.getRandomInt;

public class FieldCreatePopupTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private String organizationId;

    @BeforeClass
    public void setUp() {
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
    }

    @BeforeMethod
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6201")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "validFieldTypes")
    public void createField(FieldTypes fieldType) {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        final JSONObject fieldCreationBody = new FieldsCreationBody().bodyBuilder(fieldType);

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreationBody);
        fieldsListPage.fillCreateFieldPopupFields(fieldCreationBody);
        fieldsListPage.createNewField();
        fieldsListPage.checkCreateSuccessToast();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6225")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "validFieldTypesWithOptions")
    public void checkTrashIconState(FieldTypes fieldType) {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        final JSONObject fieldCreateBody = new FieldsCreationBody().bodyBuilder(fieldType);

        final JSONObject option = new JSONObject();
        final String internalNameForOption = getRandomInt() + " option internal";
        final String displayNameForOption = "Checkbox option display";
        option.put(INTERNAL_NAME, internalNameForOption);
        option.put(DISPLAY_NAME, displayNameForOption);
        fieldCreateBody.getJSONArray(OPTIONS).put(option);

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreateBody);

        for (int i = 0; i < fieldCreateBody.getJSONArray(OPTIONS).length() - 1; i++) {
            fieldsListPage.checkTrashIconIsDisabledAtRow(i);
        }
        fieldsListPage.fillCreateFieldPopupFields(fieldCreateBody);
        for (int i = 0; i < fieldCreateBody.getJSONArray(OPTIONS).length(); i++) {
            fieldsListPage.checkTrashIconIsEnabled(i);
        }
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6226")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "validFieldTypesWithOptions")
    public void checkAddOptionDisableState(FieldTypes fieldType) {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        final JSONObject fieldCreateBody = new FieldsCreationBody().bodyBuilder(fieldType);
        final int maxSizeOfOptions = fieldType.equals(FieldTypes.CHECKBOX) || fieldType.equals(FieldTypes.RADIOBUTTON)? 20:50;
        final JSONArray options = new JSONArray();

        for (int i = 0; i < maxSizeOfOptions; i++) {
            final JSONObject option = new JSONObject();
            final String internalNameForOption = getRandomInt() + " option internal";
            final String displayNameForOption = "Checkbox option display";
            option.put(INTERNAL_NAME, internalNameForOption);
            option.put(DISPLAY_NAME, displayNameForOption);
            options.put(option);
        }
        fieldCreateBody.put(OPTIONS, options);

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreateBody);
        fieldsListPage.fillCreateFieldPopupFields(fieldCreateBody);
        fieldsListPage.checkAddOptionButtonIsDisabled();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6227")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void createFieldWithExistingName(Role role) {
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        final JSONObject fieldWithExistingName = new FieldsCreationBody().bodyBuilder(FieldTypes.TEXT);
        fieldWithExistingName.put(INTERNAL_NAME, "City");

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.fillCreateFieldPopupFields(fieldWithExistingName);
        fieldsListPage.createNewField();
        fieldsListPage.checkFieldAlreadyExistsErrorToast();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6228")
    @Test
    public void checkEmptyFieldErrors() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        final JSONObject fieldCreateBody = new FieldsCreationBody().bodyBuilder(FieldTypes.getRandomType());

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreateBody);
        fieldsListPage.createNewField();
        fieldsListPage.checkRequiredFieldErrors();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6229")
    @Test
    public void checkSameOptionErrorMessage() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        final JSONObject fieldCreateBody = new FieldsCreationBody().bodyBuilder(FieldTypes.getRandomTypeWithOption());
        for (int i = 0; i < 2; i++) {
            final JSONObject option = new JSONObject();
            option.put(INTERNAL_NAME, "internalNameForOption");
            option.put(DISPLAY_NAME, "displayNameForOption");
            fieldCreateBody.getJSONArray(OPTIONS).put(option);
        }

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreateBody);
        fieldsListPage.fillCreateFieldPopupFields(fieldCreateBody);
        fieldsListPage.createNewField();
        fieldsListPage.duplicateOptionsErrorCheck();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6230")
    @Test
    public void checkCloseCreationPopup() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        final JSONObject fieldCreateBody = new FieldsCreationBody().bodyBuilder(FieldTypes.getRandomTypeWithOption());

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreateBody);
        fieldsListPage.fillCreateFieldPopupFields(fieldCreateBody);
        fieldsListPage.closeFieldPopupByCancel();
        fieldsListPage.checkNoToastMessageIsDisplayed();
        fieldsListPage.checkPopupIsClosed();

        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreateBody);
        fieldsListPage.fillCreateFieldPopupFields(fieldCreateBody);
        fieldsListPage.closeFieldPopup();
        fieldsListPage.checkNoToastMessageIsDisplayed();
        fieldsListPage.checkPopupIsClosed();
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6231")
    @Test
    public void createFieldAndCheckInList() {
        final String unpublishedOrganizationId = new OrganizationFlows().createUnpublishedOrganization().getString("id");

        final FieldsListPage fieldsListPage = new FieldsListPage(browserToUse, versionToBe, unpublishedOrganizationId, supportToken);
        final JSONObject fieldCreationBody = new FieldsCreationBody().bodyBuilder(FieldTypes.getRandomType());

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCreationBody);
        fieldsListPage.fillCreateFieldPopupFields(fieldCreationBody);
        fieldsListPage.createNewField();
        fieldsListPage.checkCreateSuccessToast();
        fieldsListPage.checkCustomFieldByIndex(7, fieldCreationBody);
    }

    @Xray(requirement = "PEG-5071", test = "PEG-6376")
    @Test
    public void checkOptionsWhenChangingType() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        final JSONObject fieldCheckboxType = new FieldsCreationBody().bodyBuilder(FieldTypes.CHECKBOX);
        final JSONObject fieldRadiobuttonType = new FieldsCreationBody().bodyBuilder(FieldTypes.RADIOBUTTON);

        fieldsListPage.openPage();
        fieldsListPage.openCreateFieldPopup();
        fieldsListPage.selectType(fieldCheckboxType);
        fieldsListPage.fillCreateFieldPopupFields(fieldCheckboxType);
        fieldsListPage.selectType(fieldRadiobuttonType);
        fieldsListPage.checkOptionText(0, "");
        fieldsListPage.checkTrashIconIsDisabledAtRow(0);
    }

}
