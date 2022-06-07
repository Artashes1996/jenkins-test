package pages;

import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.TestUtils;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import static org.testng.Assert.assertEquals;

public class LocationDetailsViewModePage extends BasePage<LocationDetailsViewModePage> {
    private final String PAGE_URL = UI_URI_CONSOLE + "/company/location/";

    public String locationId;

    @FindBy(css = "[data-testid='location-name']")
    private WebElement locationNameElement;

    @FindBy(css = "[data-testid='phone-number']")
    private WebElement phoneNumberElement;

    @FindBy(css = "[data-testid='description']")
    private WebElement descriptionElement;

    @FindBy(css = "[data-testid='status'] [class*='label']")
    private WebElement status;

    @FindBy(css = "[data-testid='country']")
    private WebElement countryElement;

    @FindBy(css = "[data-testid='addressLine1']")
    private WebElement addressLine1Element;

    @FindBy(css = "[data-testid='addressLine2']")
    private WebElement addressLine2Element;

    @FindBy(css = "[data-testid='timezone']")
    private WebElement timeZoneElement;

    @FindBy(css = "[data-testid='city']")
    private WebElement cityElement;

    @FindBy(css = "[data-testid='zipcode']")
    private WebElement zipCodeElement;

    @FindBy(css = "[data-testid='stateRegion']")
    private WebElement stateRegionElement;

    public LocationDetailsViewModePage(String browser, String version, String organizationId, String token, String locationId) {
        super(browser, version, organizationId, token);
        this.locationId = locationId;
    }

    public LocationDetailsViewModePage(String browser, String version, String token, String locationId) {
        super(browser, version, token);
        this.locationId = locationId;
    }

    public void checkLocationAllDetails(JSONObject location) {
        checkText(countryElement, location.getJSONObject(ADDRESS).getString(COUNTRY));
        checkText(locationNameElement, location.getString(INTERNAL_NAME));
        checkText(phoneNumberElement, location.getString(PHONE_NUMBER));
        checkText(descriptionElement, location.getString(DESCRIPTION));
        checkText(status, TestUtils.capitalize(location.getString(STATUS)));
        checkText(timeZoneElement, location.getString(TIMEZONE));
        checkText(cityElement, location.getJSONObject(ADDRESS).getString(CITY));
        checkText(zipCodeElement, location.getJSONObject(ADDRESS).getString(ZIPCODE));
        if(location.getJSONObject(ADDRESS).getString(COUNTRY).equals("Belgium") || location.getJSONObject(ADDRESS).getString(COUNTRY).equals("Germany")) {
            isElementInvisible(stateRegionElement);
        } else {
            checkText(stateRegionElement, location.getJSONObject(ADDRESS).getString(STATE_REGION));
        }
        checkText(addressLine1Element, location.getJSONObject(ADDRESS).getString(ADDRESS_LINE_1));
        if(location.getJSONObject(ADDRESS).isNull(ADDRESS_LINE_2)){
            isElementInvisible(addressLine2Element);
        } else {
            checkText(addressLine2Element, location.getJSONObject(ADDRESS).getString(ADDRESS_LINE_2));
        }
    }

    public void checkLocationDetailsWithoutAddressLine2() {
        isElementInvisible(addressLine2Element);
    }

    public void checkLocationDetailsWithoutPhoneNumber() {
            checkText(phoneNumberElement, "N/A");
    }

    public void checkLocationDetailsWithoutDescription() {
            checkText(descriptionElement, "N/A");
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + locationId + "/details";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    public void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), getPageUrl());
        waitForElement(status);
    }

}