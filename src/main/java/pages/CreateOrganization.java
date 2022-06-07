package pages;

import helpers.BaseUIHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.HashMap;
import java.util.Map;

import static configuration.Config.UI_URI_BACK_OFFICE;
import static org.testng.Assert.assertEquals;

public class CreateOrganization extends BasePage<CreateOrganization> {

    private final String PAGE_URL = UI_URI_BACK_OFFICE + "/organizations/create";
    private static final Map <Vertical, WebElement> VERTICAL = new HashMap<>();

    @FindBy(css = "[data-testid='vertical']")
    private WebElement vertical;
    @FindBy(xpath = "//*[@data-testid='Retail/Other-select-option']/div")
    private WebElement retailOtherOption;
    @FindBy(xpath = "//*[@data-testid='Healthcare-select-option']/div")
    private WebElement healthcareOption;
    @FindBy(xpath = "//*[@data-testid='Government-select-option']/div")
    private WebElement governmentOption;
    @FindBy(xpath = "//*[@data-testid='Education-select-option']/div")
    private WebElement educationOption;
    @FindBy(css = "[data-testid='internal-name']")
    private WebElement organizationName;
    @FindBy(css = "[data-testid='website-url']")
    private WebElement websiteUrl;
    @FindBy(css = "[data-testid='phone-number']")
    private WebElement phoneNumber;
    @FindBy(css = "[data-testid='upload-logo']")
    private WebElement uploadLogo;

    @FindBy(css = "[type='submit']")
    private WebElement createButton;
    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancelButton;

    @FindBy(css = "[data-testid='internal-name-error']")
    private WebElement nameErrorMessage;
    @FindBy(css = "[data-testid='website-url-error']")
    private WebElement websiteUrlErrorMessage;
    @FindBy(css = "[data-testid='phone-number-error']")
    private WebElement phoneNumberErrorMessage;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement errorToast;
    @FindBy(css = "[data-testid='danger-toast'] [class^='_message_']")
    private WebElement errorToastMessage;

    @FindBy(css = "[data-testid='success-toast'] [class^='_message_']")
    private WebElement successToastMessage;

    public enum Vertical {
        EDUCATION,
        GOVERNMENT,
        HEALTHCARE,
        RETAIL_OTHER
    }

    {
        VERTICAL.put(Vertical.EDUCATION, educationOption);
        VERTICAL.put(Vertical.GOVERNMENT, governmentOption);
        VERTICAL.put(Vertical.HEALTHCARE, healthcareOption);
        VERTICAL.put(Vertical.RETAIL_OTHER, retailOtherOption);
    }

    public enum RequiredFields {
        NAME,
        URL,
        PHONE
    }

    public HomePage createOrganization(Vertical vertical, String orgName, String siteUrl, String contactNumber) {

        type(this.organizationName, orgName);

        type(this.websiteUrl, siteUrl);
        type(this.phoneNumber, contactNumber);
        click(this.vertical);

        if(vertical!=null){
            click(VERTICAL.get(vertical));
        }
        click(createButton);

        return new HomePage(browser,version);
    }

    public void checkNameErrorMessageEmptyState() {
        final String errorMsg = "This field is required.";
        checkText(nameErrorMessage, errorMsg);
    }

    public void checkErrorMessageInvalidValues(RequiredFields fieldName) {
        switch (fieldName) {
            case URL:
                checkText(websiteUrlErrorMessage, "Invalid URL");
                break;
            case PHONE:
                checkText(phoneNumberErrorMessage, "Invalid phone number");
                break;
        }
    }

    public void checkErrorToastMessage(){

        final String text = "This organization already exists..!";
        checkText(errorToastMessage, text);
    }

    public CreateOrganization(String browser, String version, String token) {
        super(browser, version, token);
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
