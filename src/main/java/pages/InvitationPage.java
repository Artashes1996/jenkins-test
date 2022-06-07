package pages;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import static configuration.Config.UI_URI;
import static org.testng.Assert.assertEquals;

public class InvitationPage extends BasePage<InvitationPage> {

    @Getter
    @Setter
    private String PAGE_URL = UI_URI + "/invitation/";
    private final Object token;
    private final boolean isDeletedOrInvalid;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToast;

    @FindBy(css = "[data-testid='email']")
    private WebElement email;

    @FindBy(css = "[data-testid='first-name-error']")
    private WebElement firstNameError;

    @FindBy(css = "[data-testid='first-name']")
    private WebElement firstName;

    @FindBy(css = "[data-testid='last-name']")
    private WebElement lastName;

    @FindBy(css = "[data-testid='last-name-error']")
    private WebElement lastNameError;

    @FindBy(css = "[data-testid='password']")
    private WebElement password;

    @FindBy(css = "[data-testid='password-error']")
    private WebElement passwordError;

    @FindBy(css = "[data-testid='repeat-password']")
    private WebElement repeatPassword;

    @FindBy(css = "[data-testid='repeat-password-error']")
    private WebElement repeatPasswordError;

    @FindBy(css = "[data-testid='submit']")
    private WebElement submit;

    public InvitationPage(String browser, String version, Object token, boolean isDeletedOrInvalid) {

        super(browser,version);
        this.token = token;
        this.isDeletedOrInvalid = isDeletedOrInvalid;
        this.get();
    }

    public enum FieldName {
        PASSWORD,
        FIRST_NAME,
        LAST_NAME,
        REPEAT_PASSWORD
    }

    public void checkMissingFieldError(FieldName fieldName) {

        final String errorMsg = "This field is required.";

        switch (fieldName) {
            case PASSWORD:
                checkText(passwordError, errorMsg);
                break;

            case FIRST_NAME:
                checkText(firstNameError, errorMsg);
                break;

            case LAST_NAME:
                checkText(lastNameError, errorMsg);
                break;

            case REPEAT_PASSWORD:
                checkText(repeatPasswordError, errorMsg);
                break;
        }
    }

    public void checkNonMatchingPasswordError() {
        final String errorMsg = "The passwords don't match";
        checkText(repeatPasswordError, errorMsg);
    }

    public void checkNonSecurePasswordError() {
        final String errorMsg = "Please set a secure password.";
        checkText(passwordError, errorMsg);
    }

    public HomePage invitationAccept(String userFirstName, String userLastName, String userPassword, String userRepeatPass){

        type(firstName, userFirstName);
        type(lastName, userLastName);
        type(password, userPassword);
        type(repeatPassword, userRepeatPass);
        click(submit);
        return new HomePage(browser,version);
    }

    public void checkErrorToastForNonExistingDeletedInvitation(){
        checkText(dangerToast,"YOU HAVE BEEN UNINVITED.\n" +
                "Please contact your admin to get a new invitation.");
    }

    public void checkExpiredPageRedirection(){
        Assert.assertTrue(getCurrentUrl().contains("/expired")); // TODO : Need to clarify path
    }

    public void checkEmailFieldPreFill(String userEmail) {
        checkText(email, userEmail);
    }

    public void checkEmailFieldPreFill(String userEmail, String userFirstName, String userLastName) {
        checkText(email, userEmail);
        checkValue(firstName, userFirstName);
        checkValue(lastName, userLastName);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {

        if(isDeletedOrInvalid) {
            driver.get(getPageUrl() + "/signin");
        } else {
            driver.get(getPageUrl() + token);
        }
    }

    @Override
    protected void isLoaded() throws Error {
        if(isDeletedOrInvalid) {
            assertEquals(driver.getCurrentUrl(),getPageUrl() + "/signin");
        } else {
            assertEquals(driver.getCurrentUrl(),getPageUrl() + token);
        }

    }
}
