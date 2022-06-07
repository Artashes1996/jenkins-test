package e2e.ui.pages;

import configuration.Role;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;
import pages.HomePage;
import pages.InvitationPage;
import utils.RetryAnalyzer;
import helpers.DBHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import lombok.SneakyThrows;
import org.json.JSONObject;
import e2e.ui.dataProviders.InvitationDataProvider;
import utils.Xray;

import java.util.Random;

public class InvitationPageTest extends BasePageTest {

    private String organizationId;
    private static final String NAME = "Test";
    private static final String SURNAME = "QA";
    private static final String PASS = "Qw123456!";
    private static final Logger LOGGER  = Logger.getLogger(InvitationPageTest.class);

    @BeforeClass
    public void organizationDataPreparation(){
        this.organizationId = new OrganizationFlows().createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");
    }


    @Xray(requirement = "PEG-138", test = "PEG-1717")
    @Test(retryAnalyzer = RetryAnalyzer.class)

    public void checkInvalidInvitation() {

        LOGGER.trace("<--------- STEPS of PEG-1717 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, Math.abs(new Random().nextLong()) +"test", false);

        LOGGER.trace("<--------- VALIDATION of PEG-1717 -----------");
        invitationPage.checkErrorToastForNonExistingDeletedInvitation();
    }

    @SneakyThrows
    @Xray(requirement = "PEG-138", test = "PEG-1717")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkDeletedInvitationGet() {

        LOGGER.trace("<-------- PRECONDITION of PEG-1717 ----------");
        final JSONObject user = new UserFlows().inviteUser(organizationId, Role.OWNER,null);
        final String invitationToken =  user.getString("token");
        final UserFlows userFlows = new UserFlows();
        final String userId = userFlows.getUserId(user.getString("email"), organizationId);
        userFlows.deleteUser(organizationId, userId );

        LOGGER.trace("<--------- STEPS of PEG-1717 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, false);

        LOGGER.trace("<--------- VALIDATION of PEG-1717 -----------");
        invitationPage.checkErrorToastForNonExistingDeletedInvitation();
    }

    @Xray(requirement = "PEG-801", test = "PEG-1840")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkValidEmailOnlyInvitationAccept() {

        LOGGER.trace("<-------- PRECONDITION of PEG-1840 ----------");
        final JSONObject user = new UserFlows().inviteUser(organizationId, Role.OWNER,null);
        final String invitationToken =  user.getString("token");

        LOGGER.trace("<--------- STEPS of PEG-1840 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, false);
        invitationPage.checkEmailFieldPreFill(user.getString("email"));
        final HomePage homePage = invitationPage.invitationAccept(NAME, SURNAME, PASS, PASS);

        LOGGER.trace("<--------- VALIDATION of PEG-1840 -----------");
        homePage.checkHeaderActionIconText(NAME, SURNAME);
    }

    @Xray(requirement = "PEG-801", test = "PEG-1841")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void passwordsDoNotMatchError() {

        LOGGER.trace("<-------- PRECONDITION of PEG-1841 ----------");
        final String invitationToken =  new UserFlows().inviteUser(organizationId, Role.OWNER,null).getString("token");

        LOGGER.trace("<--------- STEPS of PEG-1841 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, false);
        invitationPage.invitationAccept(NAME, SURNAME, PASS, PASS +"!");

        LOGGER.trace("<--------- VALIDATION of PEG-1841 -----------");
        invitationPage.checkNonMatchingPasswordError();

    }

    @Xray(requirement = "PEG-801", test = "PEG-1842")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "invalid passwords", dataProviderClass = InvitationDataProvider.class)
    public void nonSecurePasswordOnAccept(String invalidPassword) {

        LOGGER.trace("<-------- PRECONDITION of PEG-1842 ----------");
        final String invitationToken =  new UserFlows().inviteUser(organizationId, Role.OWNER,null).getString("token");

        LOGGER.trace("<--------- STEPS of PEG-1842 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, false);
        invitationPage.invitationAccept(NAME, SURNAME, invalidPassword, invalidPassword);

        LOGGER.trace("<--------- VALIDATION of PEG-1842 -----------");
        invitationPage.checkNonSecurePasswordError();

    }

    // TODO check with Product Manager this case
    @SneakyThrows
    @Xray(requirement = "PEG-138", test = "PEG-1837")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkExpiredInvitation() {

        LOGGER.trace("<-------- PRECONDITION of PEG-1837 ----------");
        final String invitationToken = new UserFlows().inviteUser(organizationId, Role.OWNER,null).getString("token");
        DBHelper.expireInvitationToken(invitationToken);

        LOGGER.trace("<--------- STEPS of PEG-1837 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, true);

        LOGGER.trace("<--------- VALIDATION of PEG-1837 -----------");
        invitationPage.checkExpiredPageRedirection();

    }

    @Xray(requirement = "PEG-801", test = "PEG-1843")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkValidEmailFirstLastNameInvitationAccept() {

        LOGGER.trace("<-------- PRECONDITION of PEG-1843 ----------");
        final JSONObject user = new UserFlows().inviteUser(organizationId, Role.OWNER,null);
        final String invitationToken =  user.getString("token");

        LOGGER.trace("<--------- STEPS of PEG-1843 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, false);
        invitationPage.checkEmailFieldPreFill(user.getString("email"), user.getString("firstName"), user.getString("lastName"));
        final HomePage homePage = invitationPage.invitationAccept(NAME, SURNAME, PASS, PASS);

        LOGGER.trace("<--------- VALIDATION of PEG-1843 -----------");
        homePage.checkHeaderActionIconText(NAME, SURNAME);

    }

    @Xray(requirement = "PEG-801", test = "PEG-1844")
    @Test(retryAnalyzer = RetryAnalyzer.class, dataProvider = "missing invitation accept fields", dataProviderClass = InvitationDataProvider.class)
    public void missingRequiredFields(InvitationPage.FieldName fieldName, String firstName, String lastName, String pswd, String repeatPswd ) {

        LOGGER.trace("<-------- PRECONDITION of PEG-1844 ----------");
        final String invitationToken =  new UserFlows().inviteUserWithoutPOC(organizationId, Role.OWNER,null).getString("token");

        LOGGER.trace("<--------- STEPS of PEG-1844 -----------");
        final InvitationPage invitationPage = new InvitationPage(browserToUse, versionToBe, invitationToken, false);
        invitationPage.invitationAccept(firstName, lastName, pswd, repeatPswd);

        LOGGER.trace("<--------- VALIDATION of PEG-1844 -----------");
        invitationPage.checkMissingFieldError(fieldName);

    }

}
