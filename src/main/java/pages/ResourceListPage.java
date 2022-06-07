package pages;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.resourcesresource.payloads.ResourceCreationBody.*;
import static utils.TestUtils.capitalize;
import static org.testng.Assert.*;

public class ResourceListPage extends BasePage<ResourceListPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/management/resources/list";


    public ResourceListPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public ResourceListPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public ResourceListPage(String browser, String version) {
        super(browser, version);
    }

    @FindBy(css = "[data-testid='sort-INTERNAL_NAME']")
    private WebElement nameSortElement;
    @FindBy(css = "[data-testid='button-create']")
    private WebElement createResourceButton;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement nextPageButton;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement previousPageButton;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationText;
    @FindBy(css = "[data-testid='resources-search']")
    private WebElement searchInputBox;

    @FindBy(css = "[data-testid='name']")
    private List<WebElement> resourceNames;
    @FindBy(css = "[data-testid='status']")
    private List<WebElement> resourceStatuses;
    @FindBy(css = "[data-testid='actions-header']")
    private WebElement actionsHeader;
    @FindBy(css = "[class*='_action-menu-container']")
    private List<WebElement> threeDotIcons;
    @FindBy(css = "[role='menuitem']")
    private WebElement editButtonFromThreeDot;
    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResultAfterSearch;

    // Location Dropdown part
    @FindBy(css = "[data-testid='multi-select-toggle-button']")
    private WebElement locationDropDownBtn;
    @FindBy(css = "[data-testid='multi-select-search']")
    private WebElement locationDropDownSearch;
    @FindBy(css = "[data-testid='multi-select-all']")
    private WebElement locationDropDownSelectAllOption;
    @FindBy(css = "[data-testid='multi-select-predefined-option']")
    private WebElement locationDropDownNoLocationOption;
    @FindBy(css = "[data-testid='multi-select-option']")
    private List<WebElement> locationDropDownOptions;
    @FindBy(css = "[data-testid='empty-text']")
    private WebElement emptyLocationDropDownText;

    // Create Resource part
    @FindBy(css = "[data-testid='popup-title-container']")
    private WebElement createResourcePopupTitle;
    @FindBy(css = "[data-testid='popup-content'] [data-testid='internal-name']")
    private WebElement internalNameInput;
    @FindBy(css = "[data-testid='popup-content'] [data-testid='translation-name']")
    private WebElement displayNameInput;
    @FindBy(css = "[data-testid='popup-content'] [data-testid='status']")
    private WebElement statusDropdown;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastMessage;
    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToastMessage;
    @FindBy(css = "[data-testid='popup-content'] [data-testid='multi-select-toggle-button']")
    private WebElement locationDropdownInsideCreatePopup;

    // Edit Resource part
    @FindBy(css = "[data-testid='popup-content']")
    private WebElement editResourcePopupContent;
    @FindBy(css = "[class^='_internalName']")
    private WebElement internalNameText;
    @FindBy(css = "[data-testid='translation-name']")
    private WebElement editNameInput;
    @FindBy(css = "[data-testid='status']")
    private WebElement editStatusDropdown;
    @FindBy(css = "[data-testid='warning-toast']")
    private WebElement warningToast;
    @FindBy(xpath = "//*[@id='app']/div[2]/div/div[3]/button[1]")
    private WebElement warningToastYesButton;
    @FindBy(xpath = "//*[@id='app']/div[2]/div/div[3]/button[2]")
    private WebElement warningToastNoButton;

    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancel;
    @FindBy(css = "[data-testid='submit']")
    private WebElement save;


    public void checkResourceInList(int index, JSONObject resource) {
        waitForElements(resourceNames);
        checkText(resourceNames.get(index), resource.getString("internalName"));
        checkText(resourceStatuses.get(index), capitalize(resource.getString("status")));
    }

    public void checkResourcesInList(List<JSONObject> resource) {
        waitForElements(resourceNames);
        wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("[data-testid='name']"), resource.size()));
        for (int i = 0; i < resource.size(); i++) {
            checkText(resourceNames.get(i), resource.get(i).getString("internalName"));
            checkText(resourceStatuses.get(i), capitalize(resource.get(i).getString("status")));
        }
    }

    public void changeOrderOfResources() {
        click(nameSortElement);
    }

    public void checkResourceCountInPaginationText(int expectedCount) {
        isElementEnabled(paginationText);
        final String[] pieces = paginationText.getText().split(" ");
        assertEquals(pieces[pieces.length - 1], String.valueOf(expectedCount));
        isElementDisabled(previousPageButton);

        if (expectedCount > 50) {
            isElementEnabled(nextPageButton);
            click(nextPageButton);
            isElementEnabled(previousPageButton);
        } else {
            isElementDisabled(previousPageButton);
        }
    }

    public void searchForResource(String resourceName) {
        type(searchInputBox, resourceName);
    }

    public void fillLocationDropDownSearchField(String value) {
        type(locationDropDownSearch, value);
    }

    public void clearLocationDropDownSearchField() {
        clearText(locationDropDownSearch);
    }

    public void closeLocationDropdown() {
        closeByEsc(searchInputBox);
    }

    public void clickOnLocationDropDown() {
        click(locationDropDownBtn);
    }

    public void clickOnSelectAllOptionFromLocationsDropDown(int numberOfLocations) {
        click(locationDropDownSelectAllOption);
        checkText(locationDropDownBtn, numberOfLocations + " locations selected");
        waitForOptionToBeDeselected(locationDropDownNoLocationOption);
    }

    public void clickOnNoLocationsFromLocationsDropDown() {
        click(locationDropDownNoLocationOption);
        checkText(locationDropDownBtn, "No Locations");
        waitForOptionToBeDeselected(locationDropDownSelectAllOption);
    }

    public void removeAllSelectionsFromLocationsDropDown() {
        click(locationDropDownNoLocationOption);
        checkText(locationDropDownBtn, "No Locations");
        waitForOptionToBeDeselected(locationDropDownSelectAllOption);
    }

    public void unselectAllOptionsInLocationDropdown() {
        if (locationDropDownSelectAllOption.getAttribute("aria-selected").equals("true")) {
            click(locationDropDownSelectAllOption);
            waitForOptionToBeDeselected(locationDropDownSelectAllOption);
        }
        if (locationDropDownNoLocationOption.getAttribute("aria-selected").equals("true")) {
            click(locationDropDownNoLocationOption);
            waitForOptionToBeDeselected(locationDropDownNoLocationOption);
        }
    }

    private void waitForOptionToBeDeselected(WebElement option){
        wait.until(ExpectedConditions.attributeContains(option, "aria-selected", "false"));
    }

    public void clickOnOptionFromLocationsDropDownByName(String name) {
        click(findElementByText(waitForElements(locationDropDownOptions), name));
    }

    public void checkLocationDropDownItems(List<String> names) {
        names.forEach(name -> findElementByText(waitForElements(locationDropDownOptions), name));
    }

    public void checkEmptyLocationDropDownText() {
        checkText(emptyLocationDropDownText, "No locations found");
    }

    public void checkEmptySearchPageText(String searchText) {
        final String textToCompare = "No results for \"" + searchText;
        checkText(emptyResultAfterSearch, textToCompare);
    }

    public void checkNoResourceCreated() {
        final String textToCompare = "Sorry :(\nThere are no resources";
        checkText(emptyResultAfterSearch, textToCompare);
    }

    public void checkActionsColumnIsMissing() {
        isElementInvisible(actionsHeader);
    }

    public void openCreateResourcePopup() {
        click(createResourceButton);
    }

    public void clickOnLocationDropDownInsideCreatePopup() {
        click(locationDropdownInsideCreatePopup);
    }

    public void fillResourceFields(JSONObject resourceBody) {
        waitForElement(createResourcePopupTitle);
        type(internalNameInput, resourceBody.getString(INTERNAL_NAME));
        click(createResourcePopupTitle);
        checkValue(displayNameInput, resourceBody.getString(INTERNAL_NAME));

        type(displayNameInput, resourceBody.getString(NAME_TRANSLATION));
        final Select statuses = new Select(statusDropdown);
        statuses.selectByValue(resourceBody.get(STATUS).toString());
    }

    public void fillResourceFields(JSONObject resourceBody, List<String> locationNames) {
        fillResourceFields(resourceBody);
        if (locationNames != null) {
            clickOnLocationDropDownInsideCreatePopup();
            locationNames.forEach(this::clickOnOptionFromLocationsDropDownByName);
            clickOut();
        }
    }

    public void saveResourcePopup() {
        click(save);
    }

    public void closeResourcePopup() {
        click(cancel);
    }

    public void checkCreateButtonIsDisabled() {
        isElementDisabled(save);
    }

    public void checkCreateSuccessToast() {
        final String text = "SUCCESS\nThe resource has been created successfully.";
        checkText(successToastMessage, text);
    }

    public void checkAlreadyExistingResourceToast() {
        final String text = "SORRY\nA resource with this name already exists.";
        checkText(dangerToastMessage, text);
    }

    public void fillInEditResourcePopup(JSONObject resourceBody) {
        checkText(internalNameText, resourceBody.getString(INTERNAL_NAME));
        type(editNameInput, resourceBody.getString(NAME_TRANSLATION));
        clickOut();
        final Select statuses = new Select(statusDropdown);
        statuses.selectByValue(resourceBody.get(STATUS).toString());
        if (resourceBody.getString(NAME_TRANSLATION).equals(" ")){
            checkValue(editNameInput, resourceBody.getString(INTERNAL_NAME));
        }
    }

    public void checkEditConfirmationToastText() {
        final String message = "Warning!\nThese changes will affect the resource on all the linked locations. Are you sure you want to proceed?";
        checkText(warningToast, message);
    }

    public void confirmResourceChanges() {
        click(warningToastYesButton);
        isElementInvisible(warningToast);
        isElementInvisible(editResourcePopupContent);
    }

    public void rejectResourceChanges() {
        click(warningToastNoButton);
        isElementEnabled(editResourcePopupContent);
        isElementInvisible(warningToast);
    }

    public void clickThreeDotItemActionsMenu() {
        waitForElements(threeDotIcons);
        click(threeDotIcons.get(0).findElement(By.id("Capa_1")));
    }

    public void openEditPopup() {
        click(editButtonFromThreeDot);
    }

    public void checkEditSuccessToast() {
        final String text = "SUCCESS\nThe resource has been updated successfully.";
        checkText(successToastMessage, text);
    }

    public void checkPopupIsClosed() {
        isElementInvisible(cancel);
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
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(nameSortElement);
    }

}
