package pages;

import helpers.BaseUIHelper;
import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static configuration.Config.UI_URI;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ResetPasswordPage extends BasePage<ResetPasswordPage> {

    @Getter
    @Setter
    private static String PAGE_URL = UI_URI + "/reset/";
    private final Object token;

    @FindBy(css = "[data-testid='password']")
    private WebElement password;

    @FindBy(css = "[data-testid='repeat-password']")
    private WebElement repeatPass;

    @FindBy(css = "[data-testid='submit']")
    private WebElement submit;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToast;

    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToast;

    @FindBy(css = "[data-testid='password-error']")
    private WebElement passwordError;

    @FindBy(css = "[data-testid='repeat-password-error']")
    private WebElement repeatPasswordError;

    @FindBy(css = "[data-testid='subtitle']")
    private WebElement subtitle;


    public ResetPasswordPage(String browser, String version, Object token) {
        super(browser,version);
        this.token = token;
        this.get();
    }


    public enum FieldName {
        PASSWORD,
        REPEAT_PASSWORD
    }

    public void checkMissingFieldError(FieldName fieldName) {

        final String errorMsg = "This field is required.";

        switch (fieldName) {
            case PASSWORD:
                checkText(passwordError, errorMsg);
                break;

            case REPEAT_PASSWORD:
                checkText(repeatPasswordError, errorMsg);
                break;

            default:
                fail();
                break;
        }
    }

    public void resetPasswordApplyRequest(String password, String repeatPassword){
        type(this.password, password);
        type(this.repeatPass, repeatPassword);
        click(submit);
    }

    public void checkNonMatchingPasswordError() {
        final String errorMsg = "The passwords don't match";
        checkText(repeatPasswordError, errorMsg);
    }

    public void checkNonSecurePasswordError() {
        final String errorMsg = "Please set a secure password.";
        checkText(passwordError, errorMsg);
    }

    public void checkNonExistingLinkError(){
        checkText(subtitle,"Sorry, your password reset link is not existing. Please go back to the Login page or request a new one from your administrator.");
    }

    public void checkGeneralError(){
        checkText(subtitle,"We’re working on getting it fixed ASAP.");
    }

    public void checkSuccessToast(){
        checkText(successToast,"CHANGES SAVED\n" +
                "Your password has been updated!");
    }

    public void checkExpiredLinkError(){
        checkText(subtitle,"Sorry, your password reset link has expired or has been already used. Please go back to Login page or request a new one from your administrator.");
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        BaseUIHelper.getDriver(browser, version).get( getPAGE_URL() + token);
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(BaseUIHelper.getDriver(browser, version).getCurrentUrl().contains(UI_URI));
    }
}