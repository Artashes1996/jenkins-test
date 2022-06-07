package e2e.ui.pages;

import e2e.ui.dataProviders.OrganizationDataProvider;
import helpers.flows.OrganizationFlows;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;
import pages.CreateOrganization;
import pages.HomePage;
import pages.SignInPage;
import utils.RetryAnalyzer;
import utils.Xray;

public class CreateOrganizationPageTest extends BasePageTest {

    private static final Logger LOGGER = Logger.getLogger(CreateOrganizationPageTest.class);

    @BeforeMethod
    final void init() {

        LOGGER.trace("<--------- Opening Login Page for cookie setup -----------");
        new SignInPage(browserToUse, versionToBe).openPage();

    }

    @Xray(requirement = "PEG-2021", test = "PEG-2443")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "Valid Organizations", dataProviderClass = OrganizationDataProvider.class)
    public void createOrganization(CreateOrganization.Vertical vertical, String organizationName, String websiteUrl, String phone) {

        LOGGER.trace("<--------- STEPS of PEG-2443 -----------");

        final CreateOrganization createOrganization = new CreateOrganization(browserToUse, versionToBe, supportToken);
        createOrganization.openPage();
        final HomePage homePage = createOrganization.createOrganization(vertical, organizationName, websiteUrl, phone);

        LOGGER.trace("<--------- VALIDATION of PEG-2443 -----------");
   //     homePage.checkSuccessToastMessage();

        // TODO: open OrganizationListPage and check that item is there.
        // TODO: move check success toast message validation to the page, where browser is redirected

    }

    @Xray(requirement = "PEG-2021", test = "PEG-2444")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void createOrganizationWithEmptyNameField() {

        LOGGER.trace("<--------- STEPS of PEG-2444 -----------");
        final CreateOrganization createOrganization = new CreateOrganization(browserToUse, versionToBe, supportToken);
        createOrganization.openPage();
        createOrganization.createOrganization(null, "", "", "");

        LOGGER.trace("<--------- VALIDATION of PEG-2444 -----------");
        createOrganization.checkNameErrorMessageEmptyState();
    }

    @Xray(requirement = "PEG-2021", test = "PEG-2445")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "Invalid organizations with invalid values", dataProviderClass = OrganizationDataProvider.class)
    public void createOrganizationWithInvalidValues(CreateOrganization.RequiredFields fieldName, CreateOrganization.Vertical vertical, String name, String websiteUrl, String phone) {

        LOGGER.trace("<--------- STEPS of PEG-2445 -----------");

        final CreateOrganization createOrganization = new CreateOrganization(browserToUse, versionToBe, supportToken);
        createOrganization.openPage();
        createOrganization.createOrganization(vertical, name, websiteUrl, phone);

        LOGGER.trace("<--------- VALIDATION of PEG-2445 -----------");
        createOrganization.checkErrorMessageInvalidValues(fieldName);
    }

    @Xray(requirement = "PEG-2021", test = "PEG-2446")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void createOrganizationWithExistingName() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2446 ----------");
        final CreateOrganization createOrganization = new CreateOrganization(browserToUse, versionToBe, supportToken);
        createOrganization.openPage();
        final String existingOrganizationName = new OrganizationFlows().createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");

        LOGGER.trace("<--------- STEPS of PEG-2446 -----------");
        createOrganization.createOrganization(null, existingOrganizationName, "https://www.sfl.am", "+37494234264");

        LOGGER.trace("<--------- VALIDATION of PEG-2446 -----------");
        createOrganization.checkErrorToastMessage();
    }


}
