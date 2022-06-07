package pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static configuration.Config.UI_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CompanySettingsPage extends BasePage<CompanySettingsPage> {

    private final String PAGE_URL = UI_URI + "/company/management";

    @FindBy(css = "[data-testid='nav-home']")
    private WebElement goHome;
    @FindBy(css = "[data-testid='nav-management']")
    private static WebElement usersAndResources;
    @FindBy(css = "[data-testid='nav-setting']")
    private static WebElement settings;
    @FindBy(css = "[data-testid='nav-locations']")
    private static WebElement locations;
    @FindBy(css = "[data-testid='nav-services']")
    private static WebElement services;
    @FindBy(css = "[data-testid='nav-fields']")
    private static WebElement fields;


    public enum CompanyHeaderTabs {
        USERS_AND_RESOURCES(usersAndResources, "company/management"),
        SETTINGS(settings, "company/setting"),
        LOCATIONS(locations, "company/locations"),
        SERVICES(services, "company/services"),
        FIELDS(fields, "company/fields");

        private final WebElement element;
        private final String endUrl;

        CompanyHeaderTabs(WebElement element, String endUrl) {
            this.element = element;
            this.endUrl = endUrl;
        }
    }

    public BasePage navigateToTab(CompanyHeaderTabs tab) {
        click(tab.element);
        assertTrue(driver.getCurrentUrl().contains(tab.endUrl));

        return tab.equals(CompanyHeaderTabs.LOCATIONS) ? new LocationListPage(browser,version) : null;
    }

    public void checkDefaultNavigation() {
        assertTrue(driver.getCurrentUrl().contains(CompanyHeaderTabs.USERS_AND_RESOURCES.endUrl));
    }

    public CompanySettingsPage(String browser, String version) {
        super(browser, version);
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
