package e2e.ui.pages;

import e2e.ui.dataProviders.ForgotResetPassDataProvider;
import org.testng.log4testng.Logger;
import utils.RetryAnalyzer;
import configuration.Role;
import helpers.DBHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.ForgetPasswordPage;
import utils.Xray;

import java.util.Random;

public class ForgotPasswordPageTest extends BasePageTest {

    private String organizationId;

    private static final Logger LOGGER  = Logger.getLogger(ForgotPasswordPageTest.class);

    @BeforeClass
    public void dataPreparation(){
        organizationId = new OrganizationFlows().createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");

    }

    @Xray(requirement = "PEG-804", test = "PEG-1584")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void forgotPasswordForExistingAccount() {

        LOGGER.trace("<-------- PRECONDITION of PEG-1584 ----------");
        final String existingUserEmail = new UserFlows().createUser(organizationId, Role.OWNER, null).getString("email");

        LOGGER.trace("<--------- STEPS of PEG-1584 -----------");
        final ForgetPasswordPage forgotPasswordPage = new ForgetPasswordPage(browserToUse, versionToBe);
        forgotPasswordPage.openPage();
        final ForgetPasswordPage forgotPass = forgotPasswordPage.forgotPasswordRequest(existingUserEmail);

        LOGGER.trace("<--------- VALIDATION of PEG-1584 -----------");
        forgotPass.checkForgotPassSubmitLink(existingUserEmail);

    }

    @Xray(requirement = "PEG-802", test = "PEG-1831")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void forgotPasswordWithNonExistingAccount() {

        LOGGER.trace("<--------- STEPS of PEG-1831 -----------");
        final ForgetPasswordPage forgotPasswordPage = new ForgetPasswordPage(browserToUse, versionToBe);
        forgotPasswordPage.openPage();
        final ForgetPasswordPage forgotPass = forgotPasswordPage.forgotPasswordRequest(new Random().nextLong() + "qa@pegasus.com");

        LOGGER.trace("<--------- VALIDATION of PEG-1831 -----------");
        forgotPass.checkErrorToastMessage();

    }

    @Xray(requirement = "PEG-802", test = "PEG-1834")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "invalid emails", dataProviderClass = ForgotResetPassDataProvider.class)
    public void forgotPasswordWithInvalidEmail(String invalidEmail) {

        LOGGER.trace("<--------- STEPS of PEG-1834 -----------");
        final ForgetPasswordPage forgotPasswordPage = new ForgetPasswordPage(browserToUse, versionToBe);
        forgotPasswordPage.openPage();
        final ForgetPasswordPage forgotPass = forgotPasswordPage.forgotPasswordRequest(invalidEmail);

        LOGGER.trace("<--------- VALIDATION of PEG-1834 -----------");
        forgotPass.checkInvalidEmailMsg();

    }

}
