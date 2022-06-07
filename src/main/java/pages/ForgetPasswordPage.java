package pages;

import helpers.BaseUIHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import static configuration.Config.UI_URI;
import static org.testng.Assert.assertEquals;

public class ForgetPasswordPage extends BasePage<ForgetPasswordPage> {

    final private static String PAGE_URL = UI_URI + "/forgot";

    @FindBy(css = "[data-testid='email']")
    private WebElement email;

    @FindBy(css = "[data-testid='email-error']")
    private WebElement emailError;

    @FindBy(css = "[data-testid='submit']")
    private WebElement submit;

    @FindBy(xpath = "//*[@id=\"app\"]/section/div/h5")
    private WebElement resetContent;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToast;

    public ForgetPasswordPage(String browser, String version) {
        super(browser, version);
    }

    public ForgetPasswordPage forgotPasswordRequest(String forgotEmail){

        type(email,forgotEmail);
        click(submit);

        return new ForgetPasswordPage(browser,version);
    }

    public void checkForgotPassSubmitLink(String email) {

        final String text = "Instructions to reset your password have been sent to "
                + email
                + ". Please follow the link in the email to reset your password and get access to your account.";
        Assert.assertTrue(getCurrentUrl().contains(PAGE_URL));
        checkText(resetContent, text);

    }

    public void checkErrorToastMessage(){
        checkText(dangerToast,"Sorry :(\n" +
                "We were unable to reset your password. Please contact your company administrator for further assistance.");
    }

    public void checkSSOErrorToastMessage(){
        final String text = "We were unable to reset your password because you have been using SSO. Please login using your SSO provider or contact your company administrator for further assistance.";
        checkText(dangerToast,text);
    }

    public void checkInvalidEmailMsg(){
        final String emailMsg = "Invalid email";
        checkText(emailError, emailMsg);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        BaseUIHelper.getDriver(browser, version).get( PAGE_URL);
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(BaseUIHelper.getDriver(browser, version).getCurrentUrl(), PAGE_URL);
    }
}
