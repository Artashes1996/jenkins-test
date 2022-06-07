package e2e.ui.pages.fields;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.*;
import helpers.flows.FieldsFlows;
import helpers.flows.OrganizationFlows;
import org.json.*;
import org.testng.annotations.*;
import pages.FieldsListPage;
import pages.SignInPage;
import utils.Xray;

import static configuration.Role.*;
import static helpers.appsapi.fieldsresource.payloads.FieldTypes.*;
import static helpers.appsapi.fieldsresource.payloads.FieldsCreationBody.TYPE;
import static helpers.appsapi.fieldsresource.payloads.FieldsEditBody.*;

public class FieldEditPopupTest extends BasePageTest {

    private FieldsFlows fieldsFlows;
    private JSONObject organizationWithUsers;
    private JSONObject fieldWithoutOption;
    private JSONObject fieldWithOption;
    private String fieldWithoutOptionName;
    private String organizationId;
    private FieldsEditBody fieldsEditBody;

    @BeforeClass
    public void setUp() {
        fieldsFlows = new FieldsFlows();
        fieldsEditBody = new FieldsEditBody();
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final FieldTypes randomFieldTypeWithOption = getRandomTypeWithOption();
        final FieldTypes randomFieldTypeWithoutOption = getRandomTypeWithoutOption();
        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        fieldWithoutOption = fieldsFlows.createField(organizationId, randomFieldTypeWithoutOption);
        fieldWithOption = fieldsFlows.createField(organizationId, randomFieldTypeWithOption);
        fieldWithoutOptionName = fieldWithoutOption.getString("internalName");
    }

    @BeforeMethod
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6708")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void editFieldWithoutOptions(Role role) {
        final FieldTypes randomFieldTypeWithoutOption = getRandomTypeWithoutOption();
        final JSONObject createdField =
                fieldsFlows.createField(organizationId, randomFieldTypeWithoutOption);
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        final JSONObject editFieldBody = fieldsEditBody.bodyBuilder(randomFieldTypeWithoutOption);
        editFieldBody.put("type", String.valueOf(randomFieldTypeWithoutOption));
        fieldsListPage.openFieldEditPopup(createdField.getString("internalName"));
        fieldsListPage.fillFieldsInEditFieldPopup(editFieldBody);
        fieldsListPage.createNewField();
        fieldsListPage.checkUpdateSuccessToast();
        fieldsListPage.openFieldEditPopup(editFieldBody.getString(INTERNAL_NAME));
        fieldsListPage.checkFieldDetails(editFieldBody);
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6712")
    @Test
    public void closeEditFieldDetailsByCancel() {
        final Role role = getRandomAdminRole();
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithoutOptionName);
        fieldsListPage.closeFieldPopupByCancel();
        fieldsListPage.checkPopupIsClosed();
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6713")
    @Test
    public void closeEditFieldDetailsByClose() {
        final Role role = getRandomRolesWithLocation();
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithoutOptionName);
        fieldsListPage.closeFieldPopup();
        fieldsListPage.checkPopupIsClosed();
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6714")
    @Test
    public void updateNameToExistingFieldName() {
        final Role role = getRandomAdminRole();
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithoutOption.getString("internalName"));
        fieldsListPage.fillFieldsInEditFieldPopup(fieldWithOption);
        fieldsListPage.clickOnSaveButton();
        fieldsListPage.checkFieldAlreadyExistsErrorToast();
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6717")
    @Test
    public void displayNameAutoFill() {
        final Role role = getRandomAdminRole();
        final FieldTypes randomFieldTypeWithoutOption = getRandomTypeWithoutOption();
        final JSONObject createdField =
                fieldsFlows.createField(organizationId, randomFieldTypeWithoutOption);
        final String internalName = createdField.getString(INTERNAL_NAME);
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(internalName);
        fieldsListPage.checkDisplayNameAutoFill(internalName);
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6718")
    @Test
    public void checkRequiredFields() {
        final Role role = getRandomAdminRole();
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithoutOption.getString("internalName"));
        fieldsListPage.checkRequiredFields();
    }

    @Xray(requirement = "PEG-5810", test = "PEG-6722")
    @Test
    public void disableTrashIcon() {
        final Role role = getRandomAdminRole();
        final FieldTypes randomFieldTypeWithOption = FieldTypes.getRandomTypeWithOption();
        final int minimumOptionCount = randomFieldTypeWithOption.equals(CHECKBOX) ? 1 : 2;
        final JSONObject checkboxField =
                fieldsFlows.createFieldWithGivenOptionCount(organizationId, randomFieldTypeWithOption, minimumOptionCount);
        final String checkboxFieldName = checkboxField.getString("internalName");
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(checkboxFieldName);
        if (randomFieldTypeWithOption.equals(CHECKBOX)) {
            fieldsListPage.checkTrashIconIsDisabledAtRow(1);
        } else {
            fieldsListPage.checkTrashIconIsDisabledAtRow(1);
            fieldsListPage.checkTrashIconIsDisabledAtRow(2);
        }
    }

    @Xray(requirement = "PEG-5810", test = "PEG-6726")
    @Test
    public void disableAddOptionButton() {
        final Role role = getRandomAdminRole();
        final FieldTypes randomFieldTypeWithOption = getRandomTypeWithOption();
        final int maximumOptionsCount = getRandomTypeWithOption().equals(RADIOBUTTON) ? 20 :
                getRandomTypeWithOption().equals(CHECKBOX) ? 20 :
                        50;
        final JSONObject fieldWithMaximumOptions =
                fieldsFlows.createFieldWithGivenOptionCount(organizationId, randomFieldTypeWithOption, maximumOptionsCount);
        final String fieldWithMaximumOptionsName = fieldWithMaximumOptions.getString("internalName");
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithMaximumOptionsName);
        fieldsListPage.checkAddOptionButtonIsDisabled();
    }

    @Xray(requirement = "PEG-5810", test = "PEG-6728")
    @Test
    public void checkRequiredMessageForFieldWithOption() {
        final Role role = getRandomAdminRole();
        final FieldTypes randomFieldTypeWithOption = getRandomTypeWithOption();
        final JSONObject fieldWithOption =
                fieldsFlows.createFieldWithGivenOptionCount(organizationId, randomFieldTypeWithOption, 2);
        final String fieldWithOptionName = fieldWithOption.getString("internalName");
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithOptionName);
        fieldsListPage.checkRequiredFieldsInEditFieldPopup();
    }

    @Xray(requirement = "PEG-5810", test = "PEG-6729")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void editFieldWithOptions(Role role) {
        final FieldTypes randomTypeWithOption = getRandomTypeWithOption();
        final int optionsCount = 3;
        final JSONObject fieldWithOptions = fieldsFlows.createFieldWithGivenOptionCount(organizationId, randomTypeWithOption, optionsCount);
        final String userToken =
                role.equals(Role.SUPPORT)
                        ? supportToken
                        : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage =
                role.equals(Role.SUPPORT)
                        ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken)
                        : new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();

        final JSONObject editFieldBody = fieldsEditBody.bodyBuilder(randomTypeWithOption);
        final JSONArray optionsArray = editFieldBody.getJSONArray(OPTIONS_TO_MODIFY);
        editFieldBody.put(TYPE, String.valueOf(randomTypeWithOption));
        for (int i = 0; i < optionsCount; i++) {
            final JSONObject option = new JSONObject();
            option.put(OPTION_CREATION_INTERNAL_NAME, "option-" + i + "-test-edit");
            optionsArray.put(option);
        }
        fieldsListPage.openFieldEditPopup(fieldWithOptions.getString("internalName"));
        fieldsListPage.fillFieldsInEditFieldPopup(editFieldBody);
        fieldsListPage.clickOnSaveButton();
        fieldsListPage.checkUpdateSuccessToast();
        fieldsListPage.openFieldEditPopup(editFieldBody.getString(INTERNAL_NAME));
        fieldsListPage.checkFieldDetails(editFieldBody);
    }

    @Xray(requirement = "PEG-5810", test = "PEG-6841")
    @Test
    public void checkSameOptionErrorMessage() {
        final Role role = Role.getRandomAdminRole();
        final FieldTypes randomTypeWithOption = getRandomTypeWithOption();
        final JSONObject createdField = fieldsFlows.createField(organizationId, randomTypeWithOption);
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        final JSONObject editFieldBody = new FieldsEditBody().bodyBuilder(FieldTypes.getRandomTypeWithOption());
        for (int i = 0; i < 2; i++) {
            final JSONObject duplicateOption = new JSONObject();
            duplicateOption.put(INTERNAL_NAME, "duplicateOptionName");
            editFieldBody.getJSONArray(OPTIONS_TO_MODIFY).put(duplicateOption);
        }
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(createdField.getString(INTERNAL_NAME));
        fieldsListPage.clickOnAddOptionButton();
        fieldsListPage.fillFieldsInEditFieldPopup(editFieldBody);
        fieldsListPage.duplicateOptionsErrorCheck();
    }

}

