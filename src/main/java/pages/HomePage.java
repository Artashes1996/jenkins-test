package pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static configuration.Config.UI_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class HomePage extends BasePage<HomePage> {

    @FindBy(css = "[data-testid='company-settings-button']")
    private WebElement companySettings;
    @FindBy(css = "[class^='_profile_']")
    private WebElement headerNameIcon;
    @FindBy(css = "[data-testid='actionMenuItem_1']")
    private WebElement accountItem;
    @FindBy(css = "[data-testid='accountActionMenuContainer']")
    private WebElement nameAndPhotoIconContainer;
    @FindBy(css = "[data-testid='accountProfilePhoto']")
    private WebElement photoIcon;
    @FindBy(css = "[data-testid='accountFullNameContainer']")
    private WebElement fullName;
    @FindBy(css = "[data-testid='accountFirstName']")
    private WebElement firsName;
    @FindBy(css = "[data-testid='accountLastName']")
    private WebElement lastName;
    @FindBy(css = "[data-testid='actionMenuItem_3']")
    private WebElement myProfile;
    @FindBy(css = "[data-testid='actionMenuItem_4']")
    private WebElement logOut;
    @FindBy(css = "[data-testid='nav-appointments']")
    private static WebElement appointments;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastMessage;

    public enum HomePageTabs {
        APPOINTMENTS(appointments, "appointments");

        private final WebElement element;
        private final String endUrl;

        HomePageTabs(WebElement element, String endUrl) {
            this.element = element;
            this.endUrl = endUrl;
        }
    }

    public void checkTabNavigation(HomePageTabs tab) {

        click(tab.element);
        assertTrue(driver.getCurrentUrl().contains(tab.endUrl));
    }

    public CompanySettingsPage clickCompanySettings() {

        click(companySettings);
        return new CompanySettingsPage(browser,version);
    }

    public void openHeaderActionMenu(){

        click(headerNameIcon);

    }

    public void checkSuccessToastMessage(){

        final String text = "SUCCESS\nThe organization has been created successfully.!";
        checkText(successToastMessage, text);
    }

    public void checkHeaderActionIconText(String expectedFirstName, String expectedLastName){
        final String expectedLetters = expectedFirstName.charAt(0) + expectedLastName.substring(0, 1);
        checkText(headerNameIcon, expectedLetters);
    }

    public void checkNameAndIconInActionMenu(String expectedFirstName, String expectedLastName){

        checkText(photoIcon, expectedFirstName.charAt(0) + expectedLastName.substring(0, 1));
        checkText(firsName, expectedFirstName);
        checkText(lastName, expectedLastName);
    }

    public HomePage(String browser, String version) {
        super(browser, version);
    }

    @Override
    public String getPageUrl() {
        return null;
    }

    @Override
    protected void load() {
        driver.get(UI_URI);
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), UI_URI);
        waitForElement(appointments);
    }
}
