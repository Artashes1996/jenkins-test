package e2e.ui.pages;

import configuration.Role;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.SupportFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;
import pages.*;
import utils.TestUtils;
import utils.Xray;

import java.util.*;

public class HeadersTest extends BasePageTest {

    private String organizationId;
    private String ownerEmail;
    private JSONObject ownerJsonObject;
    private final String password = "Qw!123456";
    private static final Logger LOGGER  = Logger.getLogger(HeadersTest.class);


    @BeforeClass
    public void dataPreparation() {

        final JSONObject ownerAndOrganization = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = ownerAndOrganization.getJSONObject("ORGANIZATION").getString("id");
        ownerJsonObject = ownerAndOrganization.getJSONObject(Role.OWNER.toString());
        ownerEmail = ownerJsonObject.getString("email");

    }

    @Xray(requirement = "PEG-1703", test = "PEG-2123")
    @Test
    public void checkFromHomeToSettingNavigation() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2123 ----------");
        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        final HomePage homePage = (HomePage) signInPage.login(ownerEmail, password);

        LOGGER.trace("<--------- STEPS & VALIDATIONS of PEG-2123 -----------");

        homePage.checkTabNavigation(HomePage.HomePageTabs.APPOINTMENTS);
        final CompanySettingsPage companySettingsPageHeader = homePage.clickCompanySettings();
        companySettingsPageHeader.checkDefaultNavigation();
    }

    // TODO : defect opened for missing test Ids PEG-3299
    @Xray(requirement = "PEG-1703", test = "PEG-2124")
    @Test
    public void checkNavigationInSettingsTabs() {

        LOGGER.trace("<--------- STEPS of PEG-2124 -----------");
        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        final HomePage homePage = (HomePage) signInPage.login(ownerEmail, password);
        final CompanySettingsPage companySettingsPageHeader = homePage.clickCompanySettings();
        final String locationName = new LocationFlows().createLocation(organizationId).getString("internalName");
        Arrays.asList(CompanySettingsPage.CompanyHeaderTabs.values()).forEach(companySettingsPageHeader::navigateToTab);
        final LocationListPage locationListPage = (LocationListPage) companySettingsPageHeader.navigateToTab(CompanySettingsPage.CompanyHeaderTabs.LOCATIONS);

        LOGGER.trace("<--------- VALIDATION of PEG-2124 -----------");
//        locationListPage.checkAllNamesInLocationList(new ArrayList<>(Collections.singletonList(locationName)));
    }

    @Xray(requirement = "PEG-1701", test = "PEG-2506")
    @Test
    public void checkNameInConsole(){

        LOGGER.trace("<--------- STEPS of PEG-2506 -----------");
        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        final HomePage homePage = (HomePage) signInPage.login(ownerEmail, password);
        homePage.checkHeaderActionIconText(ownerJsonObject.getString("firstName"), ownerJsonObject.getString("lastName"));
        homePage.openHeaderActionMenu();

        LOGGER.trace("<--------- VALIDATION of PEG-2506 -----------");
        homePage.checkNameAndIconInActionMenu(ownerJsonObject.getString("firstName"), ownerJsonObject.getString("lastName"));
    }

    // TODO will be fixed in the scope of PEG-3004 task
    @Xray(requirement = "PEG-1701", test = "PEG-2507")
    @Test
    public void checkNameInBackOffice(){

        LOGGER.trace("<-------- PRECONDITION of PEG-2507 ----------");
        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        final JSONObject supportJsonObject = new SupportFlows().createSupport(TestUtils.getRandomInt() + "support@qless.com");

        LOGGER.trace("<--------- STEPS of PEG-2507 -----------");
        final CreateOrganization createOrganizationPage = new CreateOrganization(browserToUse, versionToBe, supportToken);
        createOrganizationPage.openPage();

        LOGGER.trace("<--------- VALIDATION of PEG-2507 -----------");
        final HomePage home = new HomePage(browserToUse,versionToBe);
        home.checkHeaderActionIconText(supportJsonObject.getString("firstName"), supportJsonObject.getString("lastName"));
        home.openHeaderActionMenu();
        home.checkNameAndIconInActionMenu(supportJsonObject.getString("firstName"), supportJsonObject.getString("lastName"));
    }

}
