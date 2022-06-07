package pages;

import helpers.BaseUIHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import static configuration.Config.UI_URI;
import static org.testng.Assert.assertEquals;

public class SignInPage extends BasePage<SignInPage> {

    final private static String PAGE_URL = UI_URI + "/signin";

    @FindBy(css = "[data-testid='email']")
    private WebElement email;

    @FindBy(css = "[data-testid='email-error']")
    private WebElement emailError;

    @FindBy(css = "[data-testid='password']")
    private WebElement password;

    @FindBy(css = "[data-testid='password-error']")
    private WebElement passError;

    @FindBy(css = "[data-testid='sign-in']")
    private WebElement signIn;

    @FindBy(css = "[data-testid='forgot-password']")
    private WebElement forgotPass;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToast;

    public SignInPage(String browser, String version) {
        super(browser, version);
    }

    public BasePage login(String userName, String pswd) {
        type(email, userName);
        type(password, pswd);
        click(signIn);

        return userName.contains("support") ? new OrganizationListPage(browser,version) : new HomePage(browser,version);
    }

    public enum FieldName {
        EMAIL,
        PASSWORD
    }

    public void checkMissingFieldError(FieldName fieldName) {

        final String errorMsg = "This field is required.";
        if (fieldName.equals(FieldName.EMAIL)) {
            checkText(emailError, errorMsg);
        } else {
            checkText(passError, errorMsg);
        }
    }

    public void checkIncorrectCredentials() {

        final String errorMsg = "SORRY :(\n" +
                "Wrong email or password";
        checkText(dangerToast, errorMsg);

    }

    public void checkForgotPassLink() {
        click(forgotPass);
        Assert.assertTrue(getCurrentUrl().contains("/forgot"));
    }

    public void checkForceReset() {
        final String forceResetErrorMessage = "SORRY :(\n" +
                "It has been requested that you change your password. Please check your email to reset the password and get access to your account.";
        checkText(dangerToast, forceResetErrorMessage);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        BaseUIHelper.getDriver(browser,version).get( PAGE_URL);
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(BaseUIHelper.getDriver(browser,version).getCurrentUrl(), PAGE_URL);
    }
}
