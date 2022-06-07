package pages;

import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;

import java.time.DayOfWeek;
import java.util.*;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;

public class LocationsAvailabilityPage extends BasePage<LocationsAvailabilityPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/locations/";

    public String locationId;
    public String userId;


    @FindBy(css = "[data-testid='input-search']")
    private WebElement searchEmployeesElement;

    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResultElement;

    @FindBy(css = "[data-testid='employee-item']")
    private List<WebElement> employeeItemElements;

    @FindBy(css = "[data-testid='employee-email']")
    private List<WebElement> employeeEmailElements;

    @FindBy(css = "[data-testid='employee-fullname']")
    private List<WebElement> employeeFullNameElements;

    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToast;

    @FindBy(css = "[data-testid*='-from-date']")
    public List<WebElement> fromDateElements;

    @FindBy(css = "[data-testid='week-day']")
    public List<WebElement> weekDayElements;

    public LocationsAvailabilityPage(String browser, String version, String locationId, String userId, String token) {
        super(browser, version, token);
        this.locationId = locationId;
        this.userId = userId;
    }

    public LocationsAvailabilityPage(String browser, String version, String organizationId, String locationId, String userId, String token) {
        super(browser, version, organizationId, token);
        this.locationId = locationId;
        this.userId = userId;
    }

    public void checkEmptyEmployeeList() {
        checkText(emptyResultElement, "Sorry :(\n" +
                "There are no employees");
    }

    public void searchEmployee(String value) {
        type(searchEmployeesElement, value);
    }

    public void checkEmployeesList(List<JSONObject> employeesList) {
        waitForElements(employeeEmailElements);
        assertEquals(employeeItemElements.size(), employeesList.size());
        for (int i = 0; i < employeesList.size(); i++) {
            checkEmployeeInfoByIndex(i, employeesList.get(i));
        }
    }

    public void checkEmployeeInfoByIndex(int index, JSONObject employee) {
        checkText(employeeEmailElements.get(index), employee.getString("email"));
        if (employee.optString("firstName").equals("") || employee.optString("lastName").equals("")) {
            checkText(employeeFullNameElements.get(index), "N/A");
        } else {
            checkText(employeeFullNameElements.get(index), employee.optString("firstName") +
                    " " + employee.optString("lastName"));
        }
    }

    public void selectAvailabilityCheckboxByWeekday(DayOfWeek dayOfWeek) {
        waitForElements(weekDayElements);
        click(driver.findElement(By.xpath("//*[@data-testid='" + dayOfWeek + "-checkbox']/parent::label")));
    }

    public void checkIfNotAvailableIsNotDisplayed(DayOfWeek dayOfWeek) {
        checkElementNotPresent(By.cssSelector("[data-testid='" + dayOfWeek + "-not-available']"));
    }

    public void checkIfNotAvailableIsDisplayed(DayOfWeek dayOfWeek) {
        isElementEnabled(driver.findElement(By.cssSelector("[data-testid='" + dayOfWeek + "-not-available']")));
    }

    public void selectHours(String fromHour, String toHour, DayOfWeek dayOfWeek, int row) {
        waitForElements(fromDateElements);
        click(driver.findElement(By.cssSelector("[data-testid='" + dayOfWeek + "-from-date-" + row + "']")));
        click(driver.findElement(By.cssSelector("[data-testid='" + fromHour + "-select-option']")));
        click(driver.findElement(By.cssSelector("[data-testid='" + dayOfWeek + "-to-date-" + row + "']")));
        click(driver.findElement(By.cssSelector("[data-testid='" + toHour + "-select-option']")));
    }

    public void byDefaultCheckboxesIsNotSelected() {
        waitForElements(weekDayElements);
        for (int i = 1; i <= DayOfWeek.values().length; i++) {
            final WebElement webElement = driver.findElement(By.cssSelector("[data-testid='"
                    + DayOfWeek.of(i) + "-checkbox']"));
            isElementNotSelected(webElement);
        }
    }

    public void byDefaultFromAndToDateValues(DayOfWeek dayOfWeek, int row) {
        waitForElements(fromDateElements);
        final WebElement fromDateElement = driver.findElement(By.cssSelector("[data-testid='" +
                dayOfWeek + "-from-date-" + row + "']"));
        final WebElement toDateElement = driver.findElement(By.cssSelector("[data-testid='" +
                dayOfWeek + "-to-date-" + row + "']"));

        checkText(fromDateElement, "9:00 am");
        checkText(toDateElement, "5:00 pm");
    }

    public void checkSuccessToast() {
        checkText(successToast, "Success\n" +
                "Changes saved");
    }

    public void checkOrderOfWeekdays() {
        waitForElements(weekDayElements);
        final ArrayList<String> weekDays = new ArrayList<>();
        for (WebElement element : weekDayElements) {
            weekDays.add(element.getText());
        }
        assertEquals(weekDays, new ArrayList<>(Arrays.asList("Sunday", "Monday",
                "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")));
    }

    public void clickOnNewIntervalButtonByWeekDay(DayOfWeek dayOfWeek) {
        waitForElements(weekDayElements);
        click(driver.findElement(By.cssSelector("[data-testid='" + dayOfWeek + "-add-interval']")));
    }

    public void clickOnRemoveIntervalButtonByWeekDayAndRow(DayOfWeek dayOfWeek, int row) {
        waitForElements(weekDayElements);
        click(driver.findElement(By.cssSelector("[data-testid='trash-" + dayOfWeek + "-" + row + "']")));
    }

    public void checkTimesByWeekdayAndRow(DayOfWeek dayOfWeek, int row, String startTime, String endTime) {
        waitForElements(weekDayElements);
        checkText(driver.findElement(By.cssSelector("[data-testid='" + dayOfWeek + "-from-date-" + row + "']")), startTime);
        checkText(driver.findElement(By.cssSelector("[data-testid='" + dayOfWeek + "-to-date-" + row + "']")), endTime);
    }


    public void checkErrorMessageByWeekdayAndRow(DayOfWeek dayOfWeek, int row, String errorMessage) {
        checkText(driver.findElement(By.cssSelector("[data-testid='error-message-" + dayOfWeek + "-" + row + "']")), errorMessage);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + locationId + "/availability/weekly?resourceId=" + userId;
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(searchEmployeesElement);
    }
}
