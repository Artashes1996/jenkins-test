package e2e.ui.pages.fields;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.flows.*;
import org.json.*;
import org.testng.annotations.*;
import pages.FieldsListPage;
import pages.SignInPage;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;

public class FieldsListPageTest extends BasePageTest {

    private JSONObject organizationWithUsers;
    private JSONObject field;
    private String organizationId;
    private JSONArray defaultFields;
    private List<JSONObject> customFields = new ArrayList<>();
    private final List<String> allServicesNames = new ArrayList<>();

    @BeforeClass
    public void setUp() {
        FieldsFlows fieldsFlows = new FieldsFlows();
        organizationWithUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationWithUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
        field = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        defaultFields = fieldsFlows.getDefaultFields(organizationId);

        for (int i = 0; i < 8; i++) {
            customFields.addAll(fieldsFlows.createAllTypesOfFields(organizationId));
        }
        customFields.add(field);
        customFields = customFields.stream().sorted(Comparator.comparing((JSONObject singleService) -> singleService.getString("internalName")))
                .collect(Collectors.toList());

        for (int i = 0; i < defaultFields.length(); i++) {
            allServicesNames.add(defaultFields.getJSONObject(i).getString("internalName"));
        }
        customFields.forEach(field -> allServicesNames.add(field.getString("internalName")));
    }

    @BeforeMethod
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-5111", test = "PEG-5838")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void checkDefaultOrderWithActions(Role role) {
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        fieldsListPage.openPage();
        fieldsListPage.checkAllDefaultFieldsOrder(defaultFields);
        fieldsListPage.checkAllCustomFieldsOrder(customFields);
        fieldsListPage.checkActionsColumnIsDisplayed();
    }

    @Xray(requirement = "PEG-5111", test = "PEG-5839")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void checkDefaultOrderWithNoAction(Role role) {
        final FieldsListPage fieldsListPage = new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        fieldsListPage.openPage();
        fieldsListPage.checkAllDefaultFieldsOrder(defaultFields);
        fieldsListPage.checkAllCustomFieldsOrder(customFields);
        fieldsListPage.checkActionsColumnIsNotDisplayed();
        fieldsListPage.checkCreateButtonIsNotDisplayed();
    }

    @Xray(requirement = "PEG-5111", test = "PEG-5840")
    @Test
    public void checkSorting() {
        final Role role = Role.getRandomRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        final List<String> defaultSortingOfNames = new ArrayList<>(allServicesNames);

        fieldsListPage.openPage();
        fieldsListPage.checkSortingOfAllFields(defaultSortingOfNames);

        fieldsListPage.changeSorting();
        Collections.sort(allServicesNames);
        fieldsListPage.checkSortingOfAllFields(allServicesNames);

        fieldsListPage.changeSorting();
        Collections.reverse(allServicesNames);
        fieldsListPage.checkSortingOfAllFields(allServicesNames);

        fieldsListPage.changeSorting();
        fieldsListPage.checkSortingOfAllFields(defaultSortingOfNames);
    }

    @Xray(requirement = "PEG-5111", test = "PEG-5842")
    @Test
    public void checkPagination() {
        final Role role = Role.getRandomRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        fieldsListPage.openPage();
        fieldsListPage.checkPaginationOnAnyPage(customFields.size() + defaultFields.length(), 1);
        fieldsListPage.checkNextEnabledPreviousDisabled();
        fieldsListPage.goToNextPage();
        fieldsListPage.checkPaginationOnAnyPage(customFields.size() + defaultFields.length(), 2);
        fieldsListPage.checkNextDisabledPreviousEnabled();
        fieldsListPage.goToPreviousPage();
        fieldsListPage.checkPaginationOnAnyPage(customFields.size() + defaultFields.length(), 1);
        fieldsListPage.checkNextEnabledPreviousDisabled();
    }

    @Xray(requirement = "PEG-5111", test = "PEG-5843")
    @Test
    public void checkSearchFunctionality() {
        final Role role = Role.getRandomRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));

        fieldsListPage.openPage();
        fieldsListPage.searchForField(customFields.get(3).getString("internalName"));
        fieldsListPage.checkCustomFieldByIndex(0, customFields.get(3));

        final String nonExistingField = "aaaaassssddddddd";
        fieldsListPage.searchForField(nonExistingField);
        fieldsListPage.checkEmptySearchResult(nonExistingField);
    }

    @Xray(requirement = "PEG-5304", test = "PEG-6719")
    @Test
    public void disableActionButtonForNonCustomFields() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        fieldsListPage.openPage();
        fieldsListPage.checkFirstSevenActionsAreDisabled();
    }

    @Xray(requirement = "PEG-6348", test = "PEG-6957")
    @Test
    public void deleteCustomField() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        fieldsListPage.openPage();
        final String fieldName = field.getString("internalName");
        fieldsListPage.searchForField(fieldName);
        fieldsListPage.clickOnActionMenuButton(0);
        fieldsListPage.clickOnDeleteButtonFromActionMenuList();
        fieldsListPage.checkWarningToastMessage();
        fieldsListPage.clickOnDeleteButton();
        fieldsListPage.checkDeleteSuccessToast();
        fieldsListPage.checkEmptySearchResult(fieldName);
    }

    @Xray(requirement = "PEG-6348", test = "PEG-6958")
    @Test
    public void cancelFieldDeleting() {
        final Role role = Role.getRandomAdminRole();
        final FieldsListPage fieldsListPage = role.equals(Role.SUPPORT) ? new FieldsListPage(browserToUse, versionToBe, organizationId, supportToken) :
                new FieldsListPage(browserToUse, versionToBe, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        fieldsListPage.openPage();
        fieldsListPage.searchForField(field.getString("internalName"));
        fieldsListPage.clickOnActionMenuButton(0);
        fieldsListPage.clickOnDeleteButtonFromActionMenuList();
        fieldsListPage.checkActionsMenuNotDisplayed();
        fieldsListPage.clickOnCancelButtonElement();
        fieldsListPage.checkWarningToastNotDisplayed();
    }

}