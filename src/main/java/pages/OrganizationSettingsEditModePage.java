package pages;

import helpers.BaseUIHelper;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.assertTrue;

public class OrganizationSettingsEditModePage extends BasePage<OrganizationSettingsEditModePage>{

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/settings/edit";

    public OrganizationSettingsEditModePage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public OrganizationSettingsEditModePage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public OrganizationSettingsEditModePage(String browser, String version) {
        super(browser, version);
    }

    @FindBy(css = "[data-testid='organization-name']")
    private WebElement organizationName;
    @FindBy(css = "[data-testid='internal-name']")
    private WebElement internalNameElement;
    @FindBy(css = "[data-testid='internal-name-error']")
    private WebElement internalNameErrorElement;
    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToastElement;
    @FindBy(css = "[data-testid='public-website']")
    private WebElement publicWebsite;
    @FindBy(css = "[data-testid='organization-vertical']")
    private WebElement vertical;
    @FindBy(css = "[data-testid='phone-number']")
    private WebElement phoneNumber;
    @FindBy(css = "[data-testid='organization-region']")
    private WebElement region;
    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancelButton;
    @FindBy(css = "[data-testid='submit']")
    private WebElement submitButton;

    @FindBy(css = "[data-testid='public-website-error']")
    private WebElement publicWebsiteUrlErrorMessage;
    @FindBy(css = "[data-testid='phone-number-error']")
    private WebElement phoneNumberErrorMessage;

    public OrganizationSettingsViewModePage editDetails(String url, String contactNumber) {
        type(publicWebsite, url);
        type(phoneNumber, contactNumber);
        click(submitButton);
        return new OrganizationSettingsViewModePage(browser, version);
    }

    public OrganizationSettingsViewModePage fillFields(JSONObject organization) {
        type(internalNameElement, organization.getString("internalName"));
        type(publicWebsite, organization.getString("websiteUrl"));
        type(phoneNumber, organization.getString("phoneNumber"));
        click(submitButton);
        return new OrganizationSettingsViewModePage(browser, version);
    }

    public void fillAllFields(String url, String contactNumber) {
        type(publicWebsite, url);
        type(phoneNumber, contactNumber);
    }

    public void checkInternalNameFieldErrorMessage() {
        type(internalNameElement, "");
        click(submitButton);
        checkText(internalNameErrorElement, "This field is required.");
    }

    public void checkDuplicationErrorToast(String internalName) {
        type(internalNameElement, internalName);
        click(submitButton);
        checkText(dangerToastElement, "Organization with this name already exists.");
    }

    public void checkInvalidUrlMessage(){
        checkText(publicWebsiteUrlErrorMessage, "Invalid url");
    }

    public void checkInvalidPhoneMessage(){
        checkText(phoneNumberErrorMessage, "Invalid phone number");
    }

    public OrganizationSettingsViewModePage discardChanges() {
        click(cancelButton);
        return new OrganizationSettingsViewModePage(browser, version);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        BaseUIHelper.getDriver(browser, version).get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(organizationName);
    }
}
