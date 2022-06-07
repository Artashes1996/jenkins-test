package pages;

import configuration.Config;
import helpers.BaseUIHelper;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.TestUtils;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;

public class OrganizationSettingsViewModePage extends BasePage<OrganizationSettingsViewModePage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/settings";

    public OrganizationSettingsViewModePage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public OrganizationSettingsViewModePage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public OrganizationSettingsViewModePage(String browser, String version) {
        super(browser, version);
    }

    @FindBy(css = "[data-testid='organization-name']")
    private WebElement organizationNameElement;
    @FindBy(css = "[data-testid='organization-status']")
    private WebElement statusElement;
    @FindBy(css = "[data-testid='organization-public-website']")
    private WebElement webSiteElement;
    @FindBy(css = "[data-testid='organization-vertical']")
    private WebElement verticalElement;
    @FindBy(css = "[data-testid='organization-phone-number']")
    private WebElement phoneNumberElement;
    @FindBy(css = "[data-testid='organization-region']")
    private WebElement regionElement;
    @FindBy(css = "[data-testid='edit-button']")
    private WebElement editButton;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastMessage;
    @FindBy(css = "[class^='_indicator-container']")
    private WebElement statusInViewAccessMode;
    @FindBy(id = "organization-status")
    private WebElement statusInDropdownView;
    @FindBy(xpath = "//*[text() = 'Pause']")
    private WebElement pauseButtonFromDropdown;
    @FindBy(xpath = "//*[text() = 'Unpause']")
    private WebElement unpauseButtonFromDropdown;

    @FindBy(css = "[data-testid='confirm']")
    private WebElement confirmButtonPopupElement;
    @FindBy(css = "[data-testid='popup-content']")
    private WebElement popupContentElement;


    public void checkOrganizationSettings(JSONObject organization) {
        final String phoneNumber = organization.optString("phoneNumber").equals(JSONObject.NULL) || organization.getString("phoneNumber").isBlank() ? "N/A"
                : organization.getString("phoneNumber");

        final String webSiteUrl = organization.optString("websiteUrl").equals(JSONObject.NULL) || organization.getString("websiteUrl").isBlank() ? "N/A"
                : organization.getString("websiteUrl");

        if (!(organization.optString("vertical").equals(""))) {
            checkText(verticalElement, TestUtils.verticalFormatter(organization.getString("vertical")));
        }
        checkText(organizationNameElement, organization.getString("internalName"));
        checkText(webSiteElement, webSiteUrl);

        checkText(phoneNumberElement, phoneNumber);
        checkText(regionElement, Config.REGION);
    }

    public void checkStatusViewAccess(String status) {
        checkText(statusInViewAccessMode, status);
    }

    public void checkStatusDropdownView(String status) {
        checkText(statusInDropdownView, status);
    }

    public OrganizationSettingsEditModePage enterEditMode() {
        click(editButton);
        return new OrganizationSettingsEditModePage(browser, version);
    }

    public void checkEditButtonIsVisible() {
        isElementEnabled(editButton);
    }

    public void checkEditButtonIsMissing() {
        isElementInvisible(editButton);
    }

    public void clickPauseOrganization() {
        click(statusInDropdownView);
        pauseButtonFromDropdown.click();
    }

    public void checkConfirmationPopupText(JSONObject organization) {
        checkText(popupContentElement, organization.getString("internalName") + "\n" +
                "If you proceed, employees and customers will not be able to continue using the platform. " +
                "Are you sure you want to proceed? Please note that this action will not affect on billing.");
    }

    public void confirmPauseOperation() {
        click(confirmButtonPopupElement);
    }

    public void checkSuccessToastWhenPausingOrganization() {
        checkText(successToastMessage, "Your organization is currently paused.");
    }

    public void clickUnpauseOrganization() {
        click(statusInDropdownView);
        unpauseButtonFromDropdown.click();
    }

    public void checkSuccessToastWhenUnpausingOrganization() {
        checkText(successToastMessage, "The organization is live.");
    }

    public void checkSuccessToastMessage() {
        checkText(successToastMessage, "Success\nChanges have been saved");
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
        waitForElement(organizationNameElement);
    }
}
