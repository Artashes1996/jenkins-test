package e2e.ui.pages;

import utils.RetryAnalyzer;
import configuration.Role;
import helpers.flows.AccountFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.SignInPage;
import e2e.ui.dataProviders.LoginDataProvider;
import utils.Xray;

import java.util.Random;

public class LoginPageTest extends BasePageTest {

    private String deletedEmail;
    private final String password = "Qw!123456";
    private String forceResetUserEmail;

    @BeforeClass
    public void dataPreparation() {
        final UserFlows userFlows = new UserFlows();
        final JSONObject organizationAndOwnerProps = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndOwnerProps.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject user = organizationAndOwnerProps.getJSONObject(Role.STAFF.toString());
        deletedEmail = user.getString("email");
        userFlows.deleteUser(organizationId, user.getString("id"));

        final JSONObject forceResetUser = userFlows.createUser(organizationId, Role.OWNER, null);
        final String forceResetUserId = forceResetUser.getString("id");
        forceResetUserEmail = forceResetUser.getString("email");

        final JSONObject inactiveUser = userFlows.createUser(organizationId, Role.OWNER,null);
        final String inactiveUserId = inactiveUser.getString("id");
        userFlows.inactivateUserById(organizationId, inactiveUserId);
        new AccountFlows().forceResetUserById(organizationId, forceResetUserId);
    }

    @Xray(requirement = "PEG-804", test = "PEG-1583")
    @Test(dataProvider = "valid users", dataProviderClass = LoginDataProvider.class, retryAnalyzer = RetryAnalyzer.class)
    final void signInWithValidCredentials(Role role) {

        final JSONObject owner = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject(Role.OWNER.name());

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        // TODO save password in variable
        signInPage.login(owner.getString("email"), "Qw!123456");

    }

    @Xray(requirement = "PEG-802", test = "PEG-1824")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkForgotPassLink() {

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        signInPage.checkForgotPassLink();
    }

    @Xray(requirement = "PEG-804", test = "PEG-1585")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void signInWithoutPassword() {

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        signInPage.login("qa@test.qa", "");
        signInPage.checkMissingFieldError(SignInPage.FieldName.PASSWORD);
    }

    @Xray(requirement = "PEG-804", test = "PEG-1585")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void signInWithoutEmail() {

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        signInPage.login("", "Qw123456");
        signInPage.checkMissingFieldError(SignInPage.FieldName.EMAIL);
    }

    @Xray(requirement = "PEG-804", test = "PEG-1823")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void deletedUserSignIn() {

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        signInPage.login(deletedEmail, password);
        signInPage.checkIncorrectCredentials();
    }

    @Xray(requirement = "PEG-804", test = "PEG-1584")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void notExistingUserSignIn() {

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        signInPage.login(new Random().nextLong() + "invalid@peg.qa", "dummy");
        signInPage.checkIncorrectCredentials();
        signInPage.checkIncorrectCredentials();
    }

    @Xray(requirement = "PEG-447", test = "PEG-1919")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void forceResetUserSignIn() {

        final SignInPage signInPage = new SignInPage(browserToUse, versionToBe);
        signInPage.openPage();
        signInPage.login(forceResetUserEmail, password);
        signInPage.checkForceReset();
    }

}