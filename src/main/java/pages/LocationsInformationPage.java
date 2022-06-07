package pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.assertTrue;

public class LocationsInformationPage extends BasePage<LocationsInformationPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/locations/";

    public String locationId;


    @FindBy(css = "[data-testid='tab-item-details']")
    private WebElement informationTabElement;

    @FindBy(css = "[data-testid='tab-item-availability']")
    private WebElement availabilityTabElement;

    public LocationsInformationPage(String browser, String version, String locationId, String token) {
        super(browser, version, token);
        this.locationId = locationId;
    }

    public void checkAvailabilityTabNotDisplayed() {
        isElementInvisible(availabilityTabElement);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + locationId + "/information";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(informationTabElement);
    }
}
