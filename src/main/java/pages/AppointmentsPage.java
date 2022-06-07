package pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;

public class AppointmentsPage extends BasePage<AppointmentsPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/appointments";
    @FindBy(xpath = "/html/body/div[1]/header/div/div/div/div")
    private WebElement avatar;
    @FindBy(xpath = "/html/body/div[1]/header/div/div/ul")
    private WebElement accountBox;
    @FindBy(xpath = "/html/body/div[1]/header/div/div/ul/li[4]")
    private WebElement logOut;

    public AppointmentsPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public AppointmentsPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public AppointmentsPage(String browser, String version) {
        super(browser, version);
    }

    public void logOut() {
        click(avatar);
        waitForElement(accountBox);
        click(logOut);
    }

    public void checkLoggedOut() {
        wait.until(ExpectedConditions.urlContains("signin"));
        assertNotNull(driver.manage().getCookieNamed("token"));
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
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
