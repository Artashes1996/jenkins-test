package e2e.ui.pages;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AppointmentsPage;
import pages.OrganizationListPage;
import pages.SignInPage;
import utils.Xray;

import static configuration.Role.*;

public class AppointmentsPageTest extends BasePageTest {

    private JSONObject organizationAndUsers;
    private String organizationId;

    @BeforeClass
    public void setUp() {
        organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @BeforeMethod
    public void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(test = "PEG-5182", requirement = "PEG-4537")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void logOutFromConsole(Role role) {
        final String token = role.equals(SUPPORT)?supportToken
                :organizationAndUsers.getJSONObject(role.name()).getString("token");
        final AppointmentsPage appointmentsPage = new AppointmentsPage(browserToUse, versionToBe, token);
        appointmentsPage.openPage();
        appointmentsPage.logOut();
        appointmentsPage.checkLoggedOut();
        appointmentsPage.clickBrowserBack();
        appointmentsPage.checkLoggedOut();
    }

    @Xray(test = "PEG-5183", requirement = "PEG-4537")
    @Test
    public void logOutFromBackOffice() {
        final String token = new AuthenticationFlowHelper().getToken(SUPPORT);
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, token);
        organizationListPage.openPage();
        organizationListPage.logOut();
        organizationListPage.checkLoggedOut();
        organizationListPage.clickBrowserBack();
        organizationListPage.checkLoggedOut();
    }

    @Xray(test = "PEG-5184", requirement = "PEG-4537")
    @Test
    public void useInvalidateTokenFromBackofficeInConsole() {
        final String token = new AuthenticationFlowHelper().getToken(SUPPORT);
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, token);
        organizationListPage.openPage();
        organizationListPage.logOut();
        final AppointmentsPage appointmentsPage = new AppointmentsPage(browserToUse, versionToBe, token);
        appointmentsPage.checkLoggedOut();
    }

    @Xray(test = "PEG-5185", requirement = "PEG-4537")
    @Test
    public void logOutSupportFromConsole() {
        final String token = new AuthenticationFlowHelper().getToken(SUPPORT);
        final AppointmentsPage appointmentsPage = new AppointmentsPage(browserToUse, versionToBe, organizationId, token);
        appointmentsPage.openPage();
        appointmentsPage.logOut();
        appointmentsPage.checkLoggedOut();
    }
}
