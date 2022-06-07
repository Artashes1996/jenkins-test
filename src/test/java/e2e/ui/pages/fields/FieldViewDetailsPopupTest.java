package e2e.ui.pages.fields;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.flows.*;
import org.json.*;
import org.testng.annotations.*;
import pages.FieldsListPage;
import pages.SignInPage;
import utils.Xray;

import static configuration.Role.*;
import static helpers.appsapi.fieldsresource.payloads.FieldTypes.*;

public class FieldViewDetailsPopupTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private JSONObject fieldWithoutOption;
    private JSONObject fieldWithOption;
    private JSONObject fieldWithMultipleOptions;
    private String fieldWithoutOptionName;
    private String fieldWithOptionName;
    private String fieldWithMultipleOptionsName;



    @BeforeClass
    public void setUp() {
        final FieldsFlows fieldsFlows = new FieldsFlows();
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        final String organizationId = organization.getString("id");
        final FieldTypes randomFieldTypeWithoutOption = getRandomTypeWithoutOption();
        final FieldTypes randomFieldTypeWithOption = getRandomTypeWithOption();
        fieldWithoutOption = fieldsFlows.createField(organizationId, randomFieldTypeWithoutOption);
        fieldWithOption = fieldsFlows.createField(organizationId, randomFieldTypeWithOption);
        fieldWithMultipleOptions = fieldsFlows.createFieldWithGivenOptionCount(organizationId, randomFieldTypeWithOption,14);
        fieldWithoutOptionName = fieldWithoutOption.getString("internalName");
        fieldWithOptionName = fieldWithOption.getString("internalName");
        fieldWithMultipleOptionsName = fieldWithMultipleOptions.getString("internalName");
    }

    @BeforeMethod
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-5409", test = "PEG-6686")
    @Test
    public void seeFieldWithoutOptionsDetailsByStaffAndLocationAdmin() {
        final Role randomRoleWithLocation = getRandomRolesWithLocation();
        final String userToken = organizationWithUsers.getJSONObject(randomRoleWithLocation.name()).getString("token");
        final FieldsListPage fieldsListPage = new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithoutOptionName);
        fieldsListPage.checkFieldDetails(fieldWithoutOption);
    }

    @Xray(requirement = "PEG-5409", test = "PEG-6687")
    @Test
    public void seeFieldWithOptionsByStaffAndLocationAdmin() {
        final Role randomRoleWithLocation = getRandomRolesWithLocation();
        final String userToken = organizationWithUsers.getJSONObject(randomRoleWithLocation.name()).getString("token");
        final FieldsListPage fieldsListPage = new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithOptionName);
        fieldsListPage.checkFieldDetails(fieldWithOption);
    }

    @Xray(requirement = "PEG-5409", test = "PEG-6692")
    @Test
    public void closeFieldDetailsPopup() {
        final Role role = getRandomRolesWithLocation();
        final String userToken = role.equals(Role.SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final FieldsListPage fieldsListPage = new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithOptionName);
        fieldsListPage.closeFieldPopup();
        fieldsListPage.checkPopupIsClosed();
    }

    @Xray(requirement = "PEG-5409", test = "PEG-6709")
    @Test
    public void checkFieldViewInCaseOfMultipleOptions() {
        final Role randomRoleWithLocation = getRandomRolesWithLocation();
        final String userToken = organizationWithUsers.getJSONObject(randomRoleWithLocation.name()).getString("token");
        final FieldsListPage fieldsListPage = new FieldsListPage(browserToUse, versionToBe, userToken);
        fieldsListPage.openPage();
        fieldsListPage.openFieldEditPopup(fieldWithMultipleOptionsName);
        fieldsListPage.checkFieldDetails(fieldWithMultipleOptions);
    }
}

