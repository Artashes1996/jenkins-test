package e2e.ui.pages;

import configuration.Role;
import e2e.ui.dataProviders.OrganizationDataProvider;
import helpers.appsapi.organizationsresource.payloads.UpdateOrganizationRequestBody;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.appsapi.support.organizationsresource.payloads.CreateOrganizationRequestBody;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.OrganizationSettingsEditModePage;
import pages.OrganizationSettingsViewModePage;
import pages.SignInPage;
import utils.Xray;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.SC_CREATED;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class OrganizationSettingsPageTest extends BasePageTest {


    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-2799", test = "PEG-3855")
    @Test
    public void organizationSettingsPageViewModeUnsupportedUser() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationAndUsers.getJSONObject("ORGANIZATION");

        final String token = organizationAndUsers.getJSONObject(getRandomRolesWithLocation().name()).getString("token");
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        organizationSettingsViewModePage.checkOrganizationSettings(organization);
        organizationSettingsViewModePage.checkStatusViewAccess("Live");
        organizationSettingsViewModePage.checkEditButtonIsMissing();
    }

    @Xray(requirement = "PEG-2799", test = "PEG-4766")
    @Test
    public void organizationSettingsPageViewModeUnpublishedAdminRole() {
        final JSONObject organization = new OrganizationFlows().createUnpublishedOrganization();
        final String organizationId = organization.getString("id");

        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken :
                new UserFlows().createUser(organizationId, role, null).getString("token");
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = new OrganizationSettingsViewModePage(browserToUse, versionToBe, organizationId, token);
        organizationSettingsViewModePage.openPage();
        organizationSettingsViewModePage.checkOrganizationSettings(organization);
        organizationSettingsViewModePage.checkStatusViewAccess("Unpublished");
        organizationSettingsViewModePage.checkEditButtonIsVisible();
    }

    @Xray(requirement = "PEG-2799", test = "PEG-3857")
    @Test
    public void organizationSettingsWithoutUrlAndContact() {
        final JSONObject organizationCreationBody = CreateOrganizationRequestBody.bodyBuilder(CreateOrganizationRequestBody.OrganizationCreateCombination.REQUIRED);
        final JSONObject organization = new JSONObject(OrganizationsHelper.createOrganization(supportToken, organizationCreationBody)
                .then().statusCode(SC_CREATED).extract().body().asString());
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = new OrganizationSettingsViewModePage(browserToUse, versionToBe, organization.getString("id"), supportToken);
        organizationSettingsViewModePage.openPage();
        organizationSettingsViewModePage.checkOrganizationSettings(organization);
    }

    @Xray(requirement = "PEG-2890", test = "PEG-4768")
    @Test
    public void changeOrganizationDetailsByValidUser() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createUnpublishedOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject updateOrganizationBody = UpdateOrganizationRequestBody.bodyBuilder();

        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken :
                organizationAndUsers.getJSONObject(role.name()).getString("token");
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = role.equals(SUPPORT) ? new OrganizationSettingsViewModePage(browserToUse, versionToBe, organizationId, token)
                : new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        final OrganizationSettingsEditModePage organizationSettingsEditModePage = organizationSettingsViewModePage.enterEditMode();
        final OrganizationSettingsViewModePage organizationSettingsViewModePageEdited = organizationSettingsEditModePage.fillFields(updateOrganizationBody);

        organizationSettingsViewModePageEdited.checkSuccessToastMessage();
        organizationSettingsViewModePage.checkOrganizationSettings(updateOrganizationBody);
    }

    @Xray(requirement = "PEG-2890", test = "PEG-4769")
    @Test(dataProviderClass = OrganizationDataProvider.class, dataProvider = "Invalid website url")
    public void editOrganizationDetailsWithInvalidUrl(String url) {
        final JSONObject organizationAndUser = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final JSONObject organization = organizationAndUser.getJSONObject("ORGANIZATION");
        final String token = new AuthenticationFlowHelper().getUserTokenByRole(organization.getString("id"), getRandomOrganizationAdminRole(), null);

        final OrganizationSettingsViewModePage organizationSettingsViewModePage = new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        final OrganizationSettingsEditModePage organizationSettingsEditModePage = organizationSettingsViewModePage.enterEditMode();
        organizationSettingsEditModePage.editDetails(url, "+37455112233");
        organizationSettingsEditModePage.checkInvalidUrlMessage();

        final OrganizationSettingsViewModePage organizationSettingsViewModePageEdited = organizationSettingsEditModePage.discardChanges();
        organizationSettingsViewModePageEdited.checkOrganizationSettings(organization);
    }

    @Xray(requirement = "PEG-2890", test = "PEG-4820")
    @Test
    public void discardChangesInOrganizationDetails() {
        final JSONObject organization = new OrganizationFlows().createUnpublishedOrganization();

        final OrganizationSettingsEditModePage organizationSettingsEditModePage = new OrganizationSettingsEditModePage(browserToUse, versionToBe, organization.getString("id"), supportToken);
        organizationSettingsEditModePage.openPage();
        organizationSettingsEditModePage.fillAllFields("https://www.amazon.com", "+37455112233");

        final OrganizationSettingsViewModePage organizationSettingsViewModePageEdited = organizationSettingsEditModePage.discardChanges();
        organizationSettingsViewModePageEdited.checkOrganizationSettings(organization);
    }

    @Xray(requirement = "PEG-2890", test = "PEG-4767")
    @Test(dataProviderClass = OrganizationDataProvider.class, dataProvider = "Invalid phone number")
    public void editOrganizationDetailsWithInvalidPhone(String phoneNumber) {
        final JSONObject organizationAndUser = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final JSONObject organization = organizationAndUser.getJSONObject("ORGANIZATION");
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = new OrganizationSettingsViewModePage(browserToUse, versionToBe, organization.getString("id"), supportToken);
        organizationSettingsViewModePage.openPage();
        final OrganizationSettingsEditModePage organizationSettingsEditModePage = organizationSettingsViewModePage.enterEditMode();
        organizationSettingsEditModePage.editDetails("https://www.tesla.com", phoneNumber);
        organizationSettingsEditModePage.checkInvalidPhoneMessage();

        final OrganizationSettingsViewModePage organizationSettingsViewModePageEdited = organizationSettingsEditModePage.discardChanges();
        organizationSettingsViewModePageEdited.checkOrganizationSettings(organization);
    }

    @Xray(requirement = "PEG-2198", test = "PEG-4770")
    @Test
    public void pauseOrganizationFromDetails() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final JSONObject organization = organizationAndUsers.getJSONObject("ORGANIZATION");
        final String organizationId = organization.getString("id");

        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken :
                new AuthenticationFlowHelper().getUserTokenByRole(organizationId, role, null);
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = role.equals(SUPPORT) ? new OrganizationSettingsViewModePage(browserToUse, versionToBe, organizationId, token)
                : new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        organizationSettingsViewModePage.clickPauseOrganization();
        organizationSettingsViewModePage.checkConfirmationPopupText(organization);
        organizationSettingsViewModePage.confirmPauseOperation();
        organizationSettingsViewModePage.checkSuccessToastWhenPausingOrganization();
        organizationSettingsViewModePage.checkStatusDropdownView("Paused");
    }

    @Xray(requirement = "PEG-2198", test = "PEG-4771")
    @Test
    public void unpauseOrganizationFromDetails() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createPausedOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken :
                new AuthenticationFlowHelper().getUserTokenByRole(organizationId, role, null);
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = role.equals(SUPPORT) ? new OrganizationSettingsViewModePage(browserToUse, versionToBe, organizationId, token)
                : new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        organizationSettingsViewModePage.clickUnpauseOrganization();
        organizationSettingsViewModePage.checkSuccessToastWhenUnpausingOrganization();
        organizationSettingsViewModePage.checkStatusDropdownView("Live");
    }

    @Xray(requirement = "PEG-7115", test = "PEG-7417")
    @Test
    public void checkInternalNameErrorMessage() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createUnpublishedOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken :
                new AuthenticationFlowHelper().getUserTokenByRole(organizationId, role, null);
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = role.equals(SUPPORT) ?
                new OrganizationSettingsViewModePage(browserToUse, versionToBe, organizationId, SUPPORT_TOKEN) :
                new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        final OrganizationSettingsEditModePage organizationSettingsEditModePage = organizationSettingsViewModePage.enterEditMode();
        organizationSettingsEditModePage.checkInternalNameFieldErrorMessage();
    }

    @Xray(requirement = "PEG-7115", test = "PEG-7419")
    @Test
    public void checkDuplicationErrorMessage() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createPausedOrganizationWithAllUsers();
        final String otherOrganizationInternalName = new OrganizationFlows().createUnpublishedOrganization().getString("internalName");
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken :
                new AuthenticationFlowHelper().getUserTokenByRole(organizationId, role, null);
        final OrganizationSettingsViewModePage organizationSettingsViewModePage = role.equals(SUPPORT) ?
                new OrganizationSettingsViewModePage(browserToUse, versionToBe, organizationId, SUPPORT_TOKEN) :
                new OrganizationSettingsViewModePage(browserToUse, versionToBe, token);
        organizationSettingsViewModePage.openPage();
        final OrganizationSettingsEditModePage organizationSettingsEditModePage = organizationSettingsViewModePage.enterEditMode();
        organizationSettingsEditModePage.checkDuplicationErrorToast(otherOrganizationInternalName);
    }

}
