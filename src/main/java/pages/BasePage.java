package pages;

import configuration.Config;
import helpers.BaseUIHelper;
import lombok.Getter;
import org.apache.commons.exec.OS;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public abstract class BasePage<T extends LoadableComponent<T>> extends LoadableComponent<T> {
    @Getter
    private final static String SUBDOMAIN = Config.UI_SUBDOMAIN;
    private final static int TIMEOUT = 60;
    protected String browser;
    protected String version;
    protected WebDriver driver;
    protected Wait<WebDriver> wait;
    protected Actions actions;
    public final ThreadLocal<JSONObject> userToEnter = new ThreadLocal<>();
    public final ThreadLocal<JSONObject> resourceToEnter = new ThreadLocal<>();

    public BasePage(String browser, String version, String organizationId, String token) {
        this(browser, version);

        final Cookie tokenToCookie = new Cookie.Builder("token", token).domain(SUBDOMAIN).build();
        final Cookie orgIdToCookie = new Cookie.Builder("organizationId", "\"" + organizationId + "\"").domain(SUBDOMAIN).build();
        driver.manage().addCookie(tokenToCookie);
        driver.manage().addCookie(orgIdToCookie);
        driver.navigate().refresh();
    }


    public BasePage(String browser, String version, String token) {
        this(browser, version);
        final Cookie tokenToCookie = new Cookie.Builder("token", token).domain(SUBDOMAIN).build();

        driver.manage().addCookie(tokenToCookie);
        driver.navigate().refresh();
    }

    public BasePage(String browser, String version) {
        this.browser = browser;
        this.version = version;

        this.driver = BaseUIHelper.getDriver(browser, version);
        this.wait = new FluentWait<>(this.driver)
                .withTimeout(Duration.ofSeconds(TIMEOUT))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        this.actions = new Actions(this.driver);
        PageFactory.initElements(this.driver, this);
        this.driver.manage().timeouts().pageLoadTimeout(TIMEOUT, SECONDS);
    }

    public void openPage() {
        this.get();
    }

    public WebElement findElementByText(java.util.List<WebElement> webElements, String text) {

        return waitForElements(webElements)
                .stream()
                .filter(webElement -> Objects.equals(webElement.getText(), text))
                .findFirst()
                .orElse(null);
    }

    public String getCurrentUrl() {
        return BaseUIHelper.getDriver(browser, version).getCurrentUrl();
    }

    public abstract String getPageUrl();

    public void refreshPage() {
        driver.navigate().refresh();
    }

    public void isElementEnabled(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        assertTrue(element.isEnabled());
    }

    public void isElementDisabled(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
        assertFalse(element.isEnabled());
    }

    public void isElementInvisible(WebElement element) {
        try {
            element.isDisplayed();
        } catch (NoSuchElementException exception) {
           return;
        }
        throw new AssertionError(element + " element is present but shouldn't be.");
    }

    public void checkElementNotPresent(By by) {
        try {
            driver.findElement(by);
        } catch (NoSuchElementException exception) {
            return;
        }
        throw new AssertionError(by + " by is present but shouldn't be.");
    }

    public void isElementSelected(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
        assertTrue(element.isSelected());
    }

    public void isElementNotSelected(WebElement element) {
        assertFalse(element.isSelected());
    }

    public void waitUrlToChangeTo(String url) {
        wait.until(ExpectedConditions.urlToBe(url));
    }

    public void type(WebElement element, String text) {
        wait.until(ExpectedConditions.visibilityOf(element));
        element.clear();
        element.sendKeys(text);
    }

    public void clearText(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
        element.clear();
    }

    public void cleanText(WebElement element) {
        final String OS = System.getProperty("os.name").toLowerCase();
        wait.until(ExpectedConditions.visibilityOf(element));
        if(OS.contains("win") || OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            element.sendKeys(Keys.chord(Keys.CONTROL,"a",Keys.DELETE));
        } else if(OS.contains("mac")) {
            element.sendKeys(Keys.chord(Keys.COMMAND, "a", Keys.BACK_SPACE));
        }
    }

    public void click(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        scrollToElement(element);
        element.click();
    }

    public void checkText(WebElement element, String text) {
        wait.until(ExpectedConditions.visibilityOf(element));
        scrollToElement(element);
        try {
            new FluentWait<>(this.driver)
                    .withTimeout(Duration.ofSeconds(1))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ExpectedConditions.textToBePresentInElement(element, text));
        } catch (Exception e) {
            assertEquals(element.getText(), text);
        }

    }

    public void checkValue(WebElement element, String text) {
        wait.until(ExpectedConditions.visibilityOf(element));
        Assert.assertEquals(element.getAttribute("value"), text);
    }

    public void waitForElement(WebElement element) {
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    public void waitForElementIsNotVisible(WebElement element) {
        wait.until(ExpectedConditions.invisibilityOf(element));
    }

    public List<WebElement> waitForElements(List<WebElement> elements) {
        return wait.until(ExpectedConditions.visibilityOfAllElements(elements));
    }

    public void waitForAttributeContaining(WebElement element, String attribute, String value) {
        if (element.getAttribute(attribute) == null) {
            throw new NullPointerException("Attribute " + attribute + "can not by found on element " + element);
        }
        wait.until(ExpectedConditions.attributeContains(element, attribute, value));
    }

    public void waitForAttributeValueToBe(WebElement element, String attribute, String value) {
        if (element.getAttribute(attribute) == null) {
            throw new NullPointerException("Attribute " + attribute + "can not by found on element " + element);
        }
        wait.until(ExpectedConditions.attributeToBe(element, attribute, value));
    }

    public void scrollToElement(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        ((JavascriptExecutor) this.driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public void hoverOnElement(WebElement element) {
        scrollToElement(element);
        actions.moveToElement(element).perform();
    }

    public void checkElementIsNotSelected(WebElement element) {
        Assert.assertFalse(element.isSelected());
    }

    public void checkElementIsSelected(WebElement element) {
        Assert.assertTrue(element.isSelected());
    }

    public void clickOut() {
        driver.findElement(By.xpath("//html")).click();
    }

    public void closeByEsc(WebElement element) {
        element.sendKeys(Keys.ESCAPE);
    }

    public void clickBrowserBack() {
        driver.navigate().back();
    }

}