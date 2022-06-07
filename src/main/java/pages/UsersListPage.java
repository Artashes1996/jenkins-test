package pages;

import lombok.Getter;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.List;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;
import static utils.TestUtils.capitalize;

public class UsersListPage extends BasePage<UsersListPage> {

    private final String PAGE_URL = "/company/management/list";

    @FindBy(css = "[data-testid='sort-FIRST_NAME']")
    private WebElement nameSortElement;
    @FindBy(css = "[data-testid='users-search']")
    private WebElement usersSearchElement;
    @FindBy(tagName = "table")
    private WebElement tableElement;
    @FindBy(css = "[data-testid='name']")
    private List<WebElement> userNameElements;
    @FindBy(css = "[data-testid='role']")
    private List<WebElement> userRoles;
    @FindBy(css = "[data-testid='email']")
    private List<WebElement> userEmails;
    @FindBy(css = "[data-testid='status']")
    private List<WebElement> userStatuses;
    @FindBy(css = "[class^='_empty-message-text_']")
    private WebElement emptyPageText;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToast;

    @FindBy(className = "_indicator-container_1gzlo_1")
    private WebElement statusElement;
    @FindBy(xpath = "//div[@class='_indicator-container_1gzlo_1']/div")
    private WebElement colorElement;

    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationText;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement previousButton;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement nextButton;

    @FindBy(css = "[data-testid='multi-select-toggle-button']")
    private WebElement locationDropDownBtn;
    @FindBy(css = "[data-testid='multi-select-search']")
    private WebElement locationDropDownSearch;
    @FindBy(css = "[data-testid='multi-select-all']")
    private WebElement locationDropDownSelectAllOption;
    @FindBy(css = "[data-testid='multi-select-option']")
    private List<WebElement> locationDropDownOptions;
    @FindBy(css = "[data-testid='empty-text']")
    private WebElement emptyLocationDropDownText;


    public void checkEmptyPage() {
        checkText(emptyPageText, "There is no any data");
    }

    public void checkUserInList(int index, String fullName, String role, String email, String status) {
        waitForElements(userNameElements);
        checkText(userNameElements.get(index), fullName);
        checkText(userRoles.get(index), capitalize(role));
        checkText(userEmails.get(index), email);
        checkText(userStatuses.get(index), capitalize(status));
    }

    public void checkUserInList(JSONObject user) {

        waitForElements(userNameElements);
        waitForElements(userEmails);
        waitForElements(userStatuses);
        assertNotNull(findElementByText(userNameElements, user.getString("firstName") + " " + user.getString("lastName")));
        assertNotNull(findElementByText(userEmails, user.getString("email")));
        assertNotNull(findElementByText(userStatuses, capitalize(user.getString("status"))));

    }

    public void checkUserIsNotInTheList(JSONObject user) {

        assertNull(findElementByText(userNameElements, user.getString("firstName") + " " + user.getString("lastName")));

    }

    public void checkSuccessToastMessage(int invitationCount) {
        String successMsg = "An invitation was sent to ";
        if (invitationCount == 1) {
            successMsg += "1 email.";
        } else {
            successMsg += invitationCount + " emails.";
        }
        checkText(successToast, successMsg);
    }

    public UsersListPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public UsersListPage(String browser, String version) {
        super(browser, version);
    }

    public UsersListPage clickOnSortByUserNameButton() {
        click(nameSortElement);
        return new UsersListPage(browser, version);
    }

    public void checkUsersCount(int count) {
        waitForElements(userNameElements);
        final String[] pieces = paginationText.getText().split(" ");
        assertEquals(pieces[pieces.length - 1], String.valueOf(count));
        isElementDisabled(previousButton);

        if (count > 50) {
            isElementEnabled(nextButton);
            click(nextButton);
            isElementEnabled(previousButton);
        } else isElementDisabled(previousButton);
    }

    public boolean checkIfColumnIsAscendingSorted(ArrayList<String> columnElements) {
        boolean isSorted = true;
        for (int i = 1; i < columnElements.size(); i++) {
            if (columnElements.get(i).equals("") || columnElements.get(i - 1).equals(""))
                continue;
            if (columnElements.get(i - 1).compareTo(columnElements.get(i)) > 0 && !columnElements.get(i).equals("") && !columnElements.get(i - 1).equals("")) {
                isSorted = false;
                break;
            }
        }
        return isSorted;
    }

    public boolean checkIfColumnIsDescendingSorted(ArrayList<String> columnElements) {
        boolean isSorted = true;
        for (int i = 1; i < columnElements.size(); i++) {
            if (columnElements.get(i).equals("") || columnElements.get(i - 1).equals(""))
                continue;
            if (columnElements.get(i - 1).compareTo(columnElements.get(i)) < 0 && !columnElements.get(i).equals("") && !columnElements.get(i - 1).equals("")) {
                isSorted = false;
                break;
            }
        }
        return isSorted;
    }

    public void checkIfUserNamesColumnIsAscendingSorted() {
        ArrayList<String> userNames = new ArrayList<>();
        for (WebElement userNameElement : userNameElements) {
            userNames.add(userNameElement.getText());
        }
        assertTrue(checkIfColumnIsAscendingSorted(userNames));
    }

    public void checkIfUserNamesColumnIsDescendingSorted() {
        ArrayList<String> userNames = new ArrayList<>();
        for (WebElement userNameElement : userNameElements) {
            userNames.add(userNameElement.getText());
        }
        assertTrue(checkIfColumnIsDescendingSorted(userNames));
    }

    public enum STATUS {

        PENDING("rgba(255, 170, 0, 1)", "Pending"),
        DELETED("rgba(255, 61, 113, 1)", "Deleted"),
        EXPIRED("rgba(255, 61, 113, 1)", "Expired"),
        INACTIVE("rgba(255, 61, 113, 1)", "Inactive"),
        ACTIVE("rgba(0, 224, 150, 1)", "Active");

        @Getter
        private final String color;
        @Getter
        private final String status;

        STATUS(String color, String status) {
            this.color = color;
            this.status = status;
        }

    }

    public void checkUserStatusWithColor(STATUS status) {
        waitForElement(statusElement);
        scrollToElement(statusElement);
        final String actualColor = colorElement.getCssValue("background-color");
        checkText(statusElement, status.getStatus());
        assertEquals(actualColor, status.getColor());
    }

    public void fillValueInSearchUsersField(String value) {
        type(usersSearchElement, value);
    }

    public void fillLocationDropDownSearchField(String value) { type(locationDropDownSearch,value);}

    public void clearLocationDropDownSearchField() { clearText(locationDropDownSearch);}

    public void clickOnLocationDropDown() { click(locationDropDownBtn);}

    public void clickOnSelectAllOptionFromLocationsDropDown() {
        click(locationDropDownSelectAllOption);
    }

    public void clickOnOptionFromLocationsDropDownByName(String name) {
        click(findElementByText(waitForElements(locationDropDownOptions), name));
    }

    public void checkDropDownItems(List<String> names) {
        names.forEach(name -> findElementByText(waitForElements(locationDropDownOptions), name));
    }

    public void checkEmptyLocationDropDownText() {
        checkText(emptyLocationDropDownText, "No locations found");
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        driver.get(UI_URI_CONSOLE + PAGE_URL);
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(UI_URI_CONSOLE + PAGE_URL));
        waitForElement(nameSortElement);
    }
}
