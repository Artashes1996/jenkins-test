package pages;

import helpers.flows.ServiceFlows;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.UUID;

import static configuration.Config.*;
import static org.testng.Assert.*;
import static utils.TestUtils.*;

public class ServicesListPage extends BasePage<ServicesListPage> {

    @FindBy(css = "[data-testid='service-name-header']")
    private WebElement headerName;
    @FindBy(css = "[data-testid='sort-INTERNAL_NAME']")
    private WebElement sortInternalName;
    @FindBy(css = "[data-testid='name']")
    private List<WebElement> names;
    @FindBy(css = "[data-testid='visibility']")
    private List<WebElement> visibilities;
    @FindBy(css = "[data-testid='duration']")
    private List<WebElement> durations;
    @FindBy(css = "[data-testid='status']")
    private List<WebElement> statuses;
    @FindBy(css = "[data-testid='actions'] button")
    private List<WebElement> actions;
    @FindBy(css = "[data-testid='button-create']")
    private WebElement createServiceButton;
    @FindBy(css = "[data-testid='paginationContainer']")
    private WebElement paginationContainer;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationContent;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement previousPageButton;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement nextPageButton;
    @FindBy(css = "[data-testid='input-search']")
    private WebElement searchInput;
    @FindBy(css = "#app > div:nth-child(2) > section > div > div > div._table-container_ao4pl_7 > div > div")
    private WebElement noServiceIndicator;
    @FindBy(css = "#app > div:nth-child(2) > section > div > div > div._table-container_ao4pl_7 > div > div")
    private WebElement searchNoResult;

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/services";
    private final ServiceFlows serviceFlow = new ServiceFlows();

    public ServicesListPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public ServicesListPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public ServicesListPage(String browser, String version) {
        super(browser, version);
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
    public void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(headerName);
    }

    public void checkServiceByIndex(int index, JSONObject service) {
        waitForElements(names);
        scrollToElement(names.get(index));
        assertEquals(names.get(index).getText(), service.getString("internalName"));

        final String visibility = serviceFlow.isServiceVisible(service) ? "Visible" : "Hidden";
        waitForElements(visibilities);
        assertEquals(visibilities.get(index).getText(), visibility);

        final int durationInMinutes = (int) Math.ceil(service.getInt("duration") / 60.0);
        final String duration = durationInMinutes + " Minutes";
        waitForElements(durations);
        assertEquals(durations.get(index).getText(), duration);
        assertEquals(statuses.get(index).getText().toLowerCase(), service.getString("status").toLowerCase());
    }

    public void checkServiceData(String serviceName, String visibility, String status, String duration) {
        waitForElements(names);
        assertEquals(names.get(0).getText(), serviceName);
        assertEquals(visibilities.get(0).getText(), visibility);
        assertEquals(statuses.get(0).getText(), status);
        assertEquals(durations.get(0).getText(), duration);
    }

    public void checkActionColumnIsPresent() {
        waitForElements(actions);
    }

    public void checkActionColumnIsNotPresent() {
        assertTrue(actions.isEmpty());
    }

    public void checkCreateServiceButtonIsNotDisplayed() {
        isElementInvisible(createServiceButton);
    }

    public void checkCreateServiceButtonIsDisplayed() {
        isElementEnabled(createServiceButton);
    }

    public void checkSorting(List<String> nameValues) {
        waitForElements(names);
        for (int i = 0; i < names.size(); i++) {
            assertEquals(names.get(i).getText(), nameValues.get(i));
        }
    }

    public void changeSorting() {
        click(sortInternalName);
    }

    public void searchServices(String serviceName) {
        type(searchInput, serviceName);
    }

    public void checkPaginationNumbers(List<JSONObject> services) {
        waitForElement(paginationContent);
        String expectedContent = "";
        final int servicesCount = services.size();
        if (servicesCount > 50) {
            expectedContent = "1 - 50 of " + servicesCount;
        } else if (services.isEmpty()) {
            expectedContent = "0 - 0 of 0";
            isElementDisabled(nextPageButton);
            isElementDisabled(previousPageButton);
        } else {
            expectedContent = "1 - " + servicesCount + " of " + servicesCount;
        }
        assertEquals(paginationContent.getText(), expectedContent);
    }

    public void checkPagination(List<JSONObject> services) {
        if (services.size() > 50) {
            isElementDisabled(previousPageButton);
            click(nextPageButton);
            waitForElements(names);
            final List<JSONObject> servicesOnNextPage = services.subList(50, services.size());
            final int serviceIndex = getRandomInt(servicesOnNextPage.size());
            checkServiceByIndex(serviceIndex, services.get(50 + serviceIndex));
        } else {
            isElementDisabled(nextPageButton);
            isElementDisabled(previousPageButton);
        }
    }

    public void checkEmptyServiceList() {
        final String noServiceText = "Sorry :(\n" +
                "There are no data";
        checkText(noServiceIndicator, noServiceText);
    }

    public void checkSearchNoResult() {
        final String search = UUID.randomUUID().toString();
        waitForElement(searchInput);
        searchInput.sendKeys(search);
        String noResultText = "No results for \"" + search + "\"";
        checkText(searchNoResult, noResultText);
    }

    public void clickOnFirstServiceName() {
        waitForElements(names);
        click(names.get(0));
    }

}
