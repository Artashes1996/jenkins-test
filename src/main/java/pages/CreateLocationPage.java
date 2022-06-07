package pages;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.TestUtils;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.LocationStatuses.INACTIVE;
import static org.testng.Assert.assertEquals;

public class CreateLocationPage extends BasePage<CreateLocationPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/locations/create";

    @FindBy(css = "[data-testid='location-name']")
    private WebElement locationNameElement;
    @FindBy(css = "[data-testid='phone-number']")
    private WebElement phoneNumberElement;
    @FindBy(css = "[data-testid='description']")
    private WebElement descriptionElement;

    @FindBy(xpath = "//*[@data-testid='status']/parent::label")
    private WebElement status;

    @FindBy(css = "[data-testid='location-name-error']")
    private WebElement locationNameErrorMessage;

    @FindBy(css = "[data-testid='phone-number-error']")
    private WebElement phoneNumberErrorMessage;

    @FindBy(css = "[type='submit']")
    private WebElement createButton;
    @FindBy(css = "[data-testid='location-cancel']")
    private WebElement cancelButton;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToast;

    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToast;

    @FindBy(css = "[data-testid='location-country']")
    private WebElement countryElement;

    //  TODO change selector
    @FindBy(className = "ql-select__menu")
    private WebElement dropDownElement;

    @FindBy(css = "[data-testid='location-address-line-1']")
    private WebElement addressLine1Element;

    @FindBy(css = "[data-testid='location-city']")
    private WebElement cityElement;

    @FindBy(css = "[data-testid='location-zipcode']")
    private WebElement zipCodeElement;

    @FindBy(css = "[data-testid='location-address-line-2']")
    private WebElement addressLine2Element;

    @FindBy(css = "[data-testid='location-state-region']")
    private WebElement stateRegionElement;

    @FindBy(css = "[data-testid='location-timezone']")
    private WebElement timeZoneElement;

    @FindBy(css = "[data-testid='location-address-line-1-error']")
    private WebElement addressLine1ErrorMessage;

    @FindBy(css = "[data-testid='location-city-error']")
    private WebElement cityErrorMessage;

    @FindBy(css = "[data-testid='stateRegion-select-error']")
    private WebElement stateErrorMessage;

    @FindBy(css = "[data-testid='location-zipcode-error']")
    private WebElement zipcodeErrorMessage;

    @FindBy(css = "[data-testid='timezone-select-error']")
    private WebElement timezoneErrorMessage;

    @FindBy(css = "[data-testid='address-options-menu-list']")
    private WebElement addressOptionsListElement;

    public void fillLocationAddressData(JSONObject location) {
        checkText(status, TestUtils.capitalize(INACTIVE.name()));
        selectCountry(location.getJSONObject("address").getString("country"));
        fillAddressLine1(location.getJSONObject("address").getString("addressLine1"), false);
        if(!(location.getJSONObject("address").optString(STATE_REGION).equals(""))) {
            selectState(location.getJSONObject("address").getString("stateRegion"));
        }
        type(cityElement, location.getJSONObject("address").getString("city"));
        type(zipCodeElement, location.getJSONObject("address").getString("zipcode"));
        selectTimezone(location.getString("timezone"));
    }

    public void fillPhoneNumber(String phoneNumber) {
        type(phoneNumberElement, phoneNumber);
    }

    public void fillLocationGeneralInfo(JSONObject location) {
        type(locationNameElement, location.getString(INTERNAL_NAME));
        type(phoneNumberElement, location.optString(PHONE_NUMBER));
        type(descriptionElement, location.optString(DESCRIPTION));
    }

    public void clickCreate() {
        click(createButton);
    }

    public void clickCancel() {
        click(cancelButton);
    }

    public void checkRequiredFieldErrorMessages() {
        final String errorMsg = "This field is required.";
        checkText(locationNameErrorMessage, errorMsg);
    }

    public void fillAddressLine1(String value, boolean autoSuggestion) {
        if (autoSuggestion) {
            type(addressLine1Element, value);
            waitForElement(addressOptionsListElement);
            click(addressOptionsListElement.findElement(By.tagName("li")));
        } else {
            type(addressLine1Element, value);
        }
    }

    public void selectCountry(String country) {
        click(countryElement);
        waitForElement(dropDownElement);
        final WebElement selectMenu = driver
                .findElement(By.cssSelector("[title='" + country + "']"));
        click(selectMenu);
    }

    public void selectState(String state) {
        click(stateRegionElement);
        waitForElement(dropDownElement);
        final WebElement selectMenu = driver
                .findElement(By.cssSelector("[title='" + state + "']"));
        click(selectMenu);
    }

    public void selectTimezone(String timeZone) {
        click(timeZoneElement);
        waitForElement(dropDownElement);
        final WebElement selectMenu = driver
                .findElement(By.cssSelector("[title='" + timeZone + "']"));
        click(selectMenu);
    }

    public void checkIfStateDropdownNotVisible() {
        isElementInvisible(stateRegionElement);
    }

    public void checkInvalidPhoneNumberErrorMsg() {
        final String errorMsg = "Invalid phone number";
        checkText(phoneNumberErrorMessage, errorMsg);
    }

    public void checkExistingLocationErrorMsg() {
        final String errorMsg = "Sorry\n" +
                "Location with this name already exists.";
        checkText(dangerToast, errorMsg);
    }

    public void checkSuccessMsg() {
        final String successMsg = "Success\n" +
                "The location has been created successfully.";
        checkText(successToast, successMsg);
    }

    public void checkEmptyFields() {
        checkText(addressLine1Element, "");
        checkText(cityElement, "");
        checkText(addressLine2Element, "");
        checkText(stateRegionElement, "Select");
        checkText(zipCodeElement, "");
        checkText(timeZoneElement, "Select timezone");
    }

    public JSONObject getAddressInfo() {
        final JSONObject addressObject = new JSONObject();
        addressObject.put(CITY, cityElement.getText());
        addressObject.put(STATE_REGION, stateRegionElement.getText());
        addressObject.put(TIMEZONE, timeZoneElement.getText());
        addressObject.put(ZIPCODE, zipCodeElement.getText());
        return addressObject;
    }

    public void checkAddressDetails(JSONObject location) {
        checkText(cityElement, location.getString(CITY));
        checkText(stateRegionElement, location.getString(STATE_REGION));
        checkText(zipCodeElement, location.getString(ZIPCODE));
        checkText(timeZoneElement, location.getString(TIMEZONE));
    }

    public CreateLocationPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        driver.get(PAGE_URL);
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), PAGE_URL);
    }
}
