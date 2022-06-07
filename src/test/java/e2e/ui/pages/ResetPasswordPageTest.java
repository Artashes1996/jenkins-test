package e2e.ui.pages;

import configuration.Role;
import e2e.ui.dataProviders.ForgotResetPassDataProvider;
import helpers.flows.AccountFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import lombok.SneakyThrows;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;
import pages.ResetPasswordPage;
import utils.Xray;

public class ResetPasswordPageTest extends BasePageTest {

    private String organizationId;
    private String password;
    private static final Logger LOGGER  = Logger.getLogger(ResetPasswordPageTest.class);


    @BeforeClass
    @SneakyThrows
    final void init() {
        password = "Qw!123456!";
        this.organizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
    }


    @Xray(requirement = "PEG-803", test = "PEG-2004")
    @Test(dataProvider = "missing fields", dataProviderClass = ForgotResetPassDataProvider.class)
    public void resetPasswordWithMissingField(ResetPasswordPage.FieldName fieldName, String password, String repeatPass) {

        LOGGER.trace("<-------- PRECONDITION of PEG-2004 ----------");
        final String newUsersResetPassToken = new AccountFlows().getUserAndUsersResetPasswordToken(organizationId).getString("TOKEN");

        LOGGER.trace("<--------- STEPS of PEG-2004 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, newUsersResetPassToken);
        resetPasswordPage.resetPasswordApplyRequest(password, repeatPass);

        LOGGER.trace("<--------- VALIDATION of PEG-2004 -----------");
        resetPasswordPage.checkMissingFieldError(fieldName);
    }

    @SneakyThrows
    @Xray(requirement = "PEG-803", test = "PEG-2005")
    @Test
    public void resetPasswordNonMatchingPasswords() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2005 ----------");
        final String newUsersResetPassToken = new AccountFlows().getUserAndUsersResetPasswordToken(organizationId).getString("TOKEN");

        LOGGER.trace("<--------- STEPS of PEG-2005 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, newUsersResetPassToken);
        resetPasswordPage.resetPasswordApplyRequest(password, "!Qw123456");

        LOGGER.trace("<--------- VALIDATION of PEG-2005 -----------");
        resetPasswordPage.checkNonMatchingPasswordError();
    }

    @Xray(requirement = "PEG-803", test = "PEG-2006")
    @Test(dataProvider = "invalid passwords", dataProviderClass = ForgotResetPassDataProvider.class)
    public void resetPasswordNonSecurePasswordError(String nonSecurePass) {

        LOGGER.trace("<-------- PRECONDITION of PEG-2006 ----------");
        final String newUsersResetPassToken = new AccountFlows().getUserAndUsersResetPasswordToken(organizationId).getString("TOKEN");

        LOGGER.trace("<--------- STEPS of PEG-2006 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, newUsersResetPassToken);
        resetPasswordPage.resetPasswordApplyRequest(nonSecurePass, nonSecurePass);

        LOGGER.trace("<--------- VALIDATION of PEG-2006 -----------");
        resetPasswordPage.checkNonSecurePasswordError();
    }

    @Xray(requirement = "PEG-803", test = "PEG-2007")
    @Test
    public void invalidResetPasswordLink() {

        LOGGER.trace("<--------- STEPS of PEG-2007 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, "invalid_link");

        LOGGER.trace("<--------- VALIDATION of PEG-2007 -----------");
        resetPasswordPage.checkNonExistingLinkError();

    }

    @Xray(requirement = "PEG-803", test = "PEG-2008")
    @Test
    public void expiredResetPasswordLink() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2008 ----------");
        final String expiredToken = new AccountFlows().getExpiredTokenOfResetPass(organizationId);

        LOGGER.trace("<--------- STEPS of PEG-2008 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, expiredToken);

        LOGGER.trace("<--------- VALIDATION of PEG-2008 -----------");
        resetPasswordPage.checkExpiredLinkError();

    }

    @SneakyThrows
    @Xray(requirement = "PEG-803", test = "PEG-2009")
    @Test
    public void overriddenResetPasswordLink() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2009 ----------");
        final String userEmail = new UserFlows().createUser(organizationId, Role.OWNER, null).getString("email");
        final String oldToken = new AccountFlows().getResetPasswordRequestToken(userEmail);
        Thread.sleep(3000);
        new AccountFlows().resetPasswordRequest(userEmail);

        LOGGER.trace("<--------- STEPS of PEG-2009 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, oldToken);

        LOGGER.trace("<--------- VALIDATION of PEG-2009 -----------");
        resetPasswordPage.checkNonExistingLinkError();
    }

    @SneakyThrows
    @Xray(requirement = "PEG-803", test = "PEG-2010")
    @Test
    public void deletedUserResetPassLink() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2010 ----------");
        final String deletedToken = new AccountFlows().getDeletedTokenOfResetPass(organizationId);

        LOGGER.trace("<--------- STEPS of PEG-2010 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, deletedToken);

        LOGGER.trace("<--------- VALIDATION of PEG-2010 -----------");
        resetPasswordPage.checkGeneralError();
    }

    @Xray(requirement = "PEG-803", test = "PEG-2011")
    @Test
    public void deletedOrganizationUserResetPassLink() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2011 ----------");
        final String organizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String resetPassToken = new AccountFlows().getUserAndUsersResetPasswordToken(organizationId).getString("TOKEN");
        new OrganizationFlows().deleteOrganization(organizationId);

        LOGGER.trace("<--------- STEPS of PEG-2011 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, resetPassToken);

        LOGGER.trace("<--------- VALIDATION of PEG-2011 -----------");
        resetPasswordPage.checkGeneralError();
    }

    @Xray(requirement = "PEG-803", test = "PEG-2012")
    @Test
    public void blockedOrganizationUserResetPassLink() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2012 ----------");
        final String organizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String resetPassToken = new AccountFlows().getUserAndUsersResetPasswordToken(organizationId).getString("TOKEN");
        new OrganizationFlows().blockOrganization(organizationId);

        LOGGER.trace("<--------- STEPS of PEG-2012 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, resetPassToken);
        resetPasswordPage.resetPasswordApplyRequest(password, password);

        LOGGER.trace("<--------- VALIDATION of PEG-2012 -----------");
        resetPasswordPage.checkSuccessToast();
    }


    @Xray(requirement = "PEG-803", test = "PEG-2013")
    @Test
    public void resetPasswordWithValidPassword() {

        LOGGER.trace("<-------- PRECONDITION of PEG-2006 ----------");
        final String newUsersResetPassToken = new AccountFlows().getUserAndUsersResetPasswordToken(organizationId).getString("TOKEN");

        LOGGER.trace("<--------- STEPS of PEG-2012 -----------");
        final ResetPasswordPage resetPasswordPage = new ResetPasswordPage(browserToUse, versionToBe, newUsersResetPassToken);
        resetPasswordPage.resetPasswordApplyRequest(password, password);

        LOGGER.trace("<--------- VALIDATION of PEG-2012 -----------");
        resetPasswordPage.checkSuccessToast();
    }
}
