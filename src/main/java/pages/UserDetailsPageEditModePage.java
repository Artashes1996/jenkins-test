package pages;

import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;

public class UserDetailsPageEditModePage extends BasePage<UserDetailsPageEditModePage> {

    public UserDetailsPageEditModePage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public UserDetailsPageEditModePage(String browser, String version) {
        super(browser, version);
    }

    public UserDetailsPageEditModePage(String browser, String version, String token) {
        super(browser, version, token);
    }

    private static final String PAGE_URL = UI_URI_CONSOLE + "/company/user/";

    @FindBy(css = "[data-testid='first-name']")
    private WebElement firstNameElement;
    @FindBy(css = "[data-testid='last-name']")
    private WebElement lastNameElement;
    @FindBy(xpath = "//input[@data-testid='contact-person-switcher']/..")
    private WebElement contactPersonSwitcher;
    @FindBy(css = "[data-testid='contact-number']")
    private WebElement contactNumberElement;
    @FindBy(css = "[data-testid='discard']")
    private WebElement discardButton;
    @FindBy(css = "[data-testid='save']")
    private WebElement saveButton;
    @FindBy(css = "[data-testid='resend-invitation-button']")
    private WebElement resendInvitationButton;
    @FindBy(css = "[class='_indicator-container_1gzlo_1']")
    private WebElement userStatusElement;
    @FindBy(xpath = "//input[@data-testid='user-status-switcher']/..")
    private WebElement userStatusSwitcherElement;
    @FindBy(css = "[data-testid='user-status-switcher']")
    private WebElement userStatusStateElement;
    @FindBy(css = "[data-testid='roles']")
    private WebElement rolesElement;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastElement;
    @FindBy(css = "[data-testid='user-email']")
    private WebElement userEmail;

    @FindBy(css = "[data-testid='multi-select-toggle-button']")
    private WebElement selectLocationElement;
    @FindBy(css = "[data-testid='multi-select-search']")
    private WebElement locationSearchElement;
    @FindBy(css = "[class*='error-message']")
    private WebElement requiredFieldErrorElement;
    @FindBy(css = "[data-testid='multi-select-all']")
    private WebElement locationsSelectAllElement;
    @FindBy(css = "[data-testid='multi-select-option']")
    private List<WebElement> locationOptionElements;

    @FindBy(css = "[data-testid='first-name-error']")
    private WebElement firstNameErrorElement;
    @FindBy(css = "[data-testid='last-name-error']")
    private WebElement lastNameErrorElement;
    @FindBy(css = "[data-testid='contact-number-error']")
    private WebElement contactPersonErrorElement;

    public void editUserDetails(JSONObject editBody) {
        waitForElement(saveButton);
        click(contactPersonSwitcher);

        firstNameElement.clear();
        firstNameElement.sendKeys(editBody.getString("firstName"));
        lastNameElement.clear();
        lastNameElement.sendKeys(editBody.getString("lastName"));
        contactNumberElement.sendKeys(editBody.getString("contactNumber"));

        click(saveButton);
        final String toastMessage = "Changes have been saved";
        checkText(successToastElement, toastMessage);
    }

    public void checkRequiredFields() {
        waitForElement(saveButton);
        firstNameElement.clear();
        lastNameElement.clear();
        if (!contactPersonSwitcher.isSelected()) {
            click(contactPersonSwitcher);
        }
        contactNumberElement.clear();
        click(saveButton);
        final String errorMessage = "This field is required.";
        checkText(firstNameErrorElement, errorMessage);
        checkText(lastNameErrorElement, errorMessage);
        checkText(contactPersonErrorElement, errorMessage);
        click(contactPersonSwitcher);
    }

    @SneakyThrows
    public void changeUserStatus() {
        waitForElement(userStatusSwitcherElement);
        click(userStatusSwitcherElement);
//        TODO I'll remove this sleep after the attribute logic will be changed
        Thread.sleep(1600);
        click(saveButton);
    }

    public void changeUserRoleTo(String role, JSONArray locations) {
        scrollToElement(rolesElement);
        final Select roles = new Select(rolesElement);
        final WebElement option = roles.getOptions().stream()
                .filter(roleOption -> roleOption.getText().equals(role)).findFirst().orElse(null);
        click(option);
        if (locations != null) {
            waitForElement(selectLocationElement);
            click(selectLocationElement);
            if (locations.length() == locationOptionElements.size()) {
                selectAllLocations();
            } else {
                locations.forEach(location -> {
                    final String locationName = ((JSONObject) location).getString("internalName");
                    selectLocation(locationName);
                });
            }
            clickOut();
        }
        saveButton.click();
    }

    private void selectAllLocations() {
        waitForElement(locationsSelectAllElement);
        locationsSelectAllElement.click();
        clickOut();
    }

    public void removeCommonLocations() {
        waitForElement(selectLocationElement);
        selectLocationElement.click();
        selectAllLocations();
        selectLocationElement.click();
        if (locationsSelectAllElement.getAttribute("aria-selected").equals("true")) {
            selectAllLocations();
        }
        clickOut();
        saveButton.click();
        waitForElement(successToastElement);
        final String successToastMessage = "Changes have been saved";
        checkText(successToastElement, successToastMessage);
    }

    private void selectLocation(String locationName) {
        waitForElement(locationOptionElements.get(0));
        locationOptionElements.stream().filter(locationOptionElement -> {
            scrollToElement(locationOptionElement);
            return locationOptionElement.getText().equals(locationName);
        }).collect(Collectors.toList()).get(0).click();
    }

    public void checkLocationErrorMessage() {
        saveButton.click();
        waitForElement(requiredFieldErrorElement);
        final String errorMessageText = "This field is required.";
        checkText(requiredFieldErrorElement, errorMessageText);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + userToEnter.get().getString("id") + "/details/edit";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), getPageUrl());
    }
}
