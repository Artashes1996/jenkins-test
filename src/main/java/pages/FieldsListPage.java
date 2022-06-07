package pages;

import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import org.json.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

import java.util.List;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.fieldsresource.payloads.FieldTypes.*;
import static helpers.appsapi.fieldsresource.payloads.FieldsCreationBody.*;
import static helpers.appsapi.resourcesresource.payloads.ResourceCreationBody.INTERNAL_NAME;
import static org.testng.Assert.*;
import static utils.TestUtils.*;

public class FieldsListPage extends BasePage<FieldsListPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/fields";
    private final String REQUIRED_MESSAGE = "This field is required";

    public FieldsListPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public FieldsListPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement paginationPreviousButton;

    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement paginationNextButton;

    @FindBy(css = "[data-testid='input-search']")
    private WebElement searchInput;

    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationTextContainer;

    @FindBy(css = "[data-testid='sort-INTERNAL_NAME']")
    private WebElement nameSortElement;

    @FindBy(css = "[data-testid='fields-actions']")
    private WebElement actionsColumn;

    @FindBy(css = "[data-testid='field-name']")
    private List<WebElement> fieldNameValues;

    @FindBy(css = "[data-testid='field-type']")
    private List<WebElement> fieldTypeValues;

    @FindBy(css = "[data-testid='actions-header']") // TODO add correct selector
    private List<WebElement> fieldActions;

    @FindBy(css = "[data-testid='action-menu']") // TODO add correct selector
    private List<WebElement> actionMenuElements;

    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptySearchResult;

    @FindBy(css = "[data-testid='button-create']")
    private WebElement createNewFieldButton;

    @FindBy(css = "[data-testid='popup-content-container']")
    private WebElement createNewFieldPopup;

    @FindBy(css = "[data-testid='popup-header']")
    private WebElement createNewFieldPopupHeader;

    @FindBy(css = "[data-testid='select-control']")
    private WebElement typeDropdown;

    @FindBy(css = "[data-testid='field-internal-name']")
    private WebElement internalNameField;

    @FindBy(css = "[id='field-name']")
    private WebElement displayNameField;

    @FindBy(css = "[data-testid^='option-name-']")
    private List<WebElement> fieldOptionNames;

    @FindBy(css = "[data-testid^='trash-']")
    private List<WebElement> trashIconElements;

    @FindBy(css = "[data-testid='addOption']")
    private WebElement addOptionButton;

    @FindBy(css = "[data-testid='popup-close-btn']")
    private WebElement popUpCloseButton;

    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancelButton;

    @FindBy(css = "[data-testid='submit']")
    private WebElement submitButtonOnPopup;

    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToastMessage;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToastMessage;

    @FindBy(css = "[data-testid$='-error']")
    private List<WebElement> optionNameError;

    @FindBy(css = "[data-testid='field-internal-name-error']")
    private WebElement internalNameRequiredError;

    @FindBy(css = "[data-testid='field-display-name-error']")
    private WebElement displayNameRequiredError;

    @FindBy(css = "[data-testid='field-type-label']")
    private WebElement fieldTypePopupElement;

    @FindBy(css = "[data-testid='field-internal-name']")
    private WebElement internalNamePopupElement;

    @FindBy(css = "[data-testid='field-display-name']")
    private WebElement displayNamePopupElement;

    @FindBy(css = "[data-testid^='option ']")
    private List<WebElement> optionNamePopupElements;

    @FindBy(css = "button[disabled][data-testid='action-menu']")
    private List<WebElement> disabledActionFieldElements;

    @FindBy(css = "[data-testid='action-menu-list-item-delete']")
    private WebElement fieldItemDeleteButtonElement;

    @FindBy(css = "[data-testid='action-menu-list']")
    private WebElement actionMenuListElement;

    @FindBy(css = "[data-testid='confirm']")
    private WebElement deleteButtonElement;

    @FindBy(css = "[data-testid='dismiss']")
    private WebElement cancelButtonToastElement;

    @FindBy(css = "[data-testid='warning-toast']")
    private WebElement warningToastElement;


    public void checkPaginationOnAnyPage(int totalElementCount, int page) {
        final int maxPageAmount = 50;
        waitForElement(paginationTextContainer);
        final int fromNumber = (page - 1) * maxPageAmount + 1;
        final int toNumber = Math.min((page - 1) * maxPageAmount + 50, totalElementCount);
        final String expectedContent = fromNumber + " - " + toNumber + " of " + totalElementCount;
        checkText(paginationTextContainer, expectedContent);
    }

    public void checkNextEnabledPreviousDisabled() {
        isElementEnabled(paginationNextButton);
        isElementDisabled(paginationPreviousButton);
    }

    public void checkNextEnabledPreviousEnabled() {
        isElementEnabled(paginationPreviousButton);
        isElementEnabled(paginationNextButton);
    }

    public void checkNextDisabledPreviousEnabled() {
        isElementDisabled(paginationNextButton);
        isElementEnabled(paginationPreviousButton);
    }

    public void goToNextPage() {
        click(paginationNextButton);
    }

    public void goToPreviousPage() {
        click(paginationPreviousButton);
    }

    public void checkDefaultFieldByIndex(int index, JSONObject field) {
        waitForElements(fieldNameValues);
        scrollToElement(fieldNameValues.get(index));
        assertEquals(fieldNameValues.get(index).getText(), field.get("internalName"));
        assertEquals(fieldTypeValues.get(index).getText(), capitalize(FieldTypes.TEXT.name()));
    }

    public void checkAllDefaultFieldsOrder(JSONArray allFields) {
        final int sizeToCheck = 7;
        for (int i = 0; i < sizeToCheck; i++) {
            checkDefaultFieldByIndex(i, allFields.getJSONObject(i));
        }
    }

    public void checkCustomFieldByIndex(int index, JSONObject field) {
        waitForElements(fieldNameValues);
        scrollToElement(fieldNameValues.get(index));
        assertEquals(fieldNameValues.get(index).getText(), field.getString("internalName"));
        assertEquals(fieldTypeValues.get(index).getText(), verticalFormatter(field.getString("type")));
    }

    public void checkAllCustomFieldsOrder(List<JSONObject> allFields) {
        final int defaultFieldCount = 7;
        final int sizeToCheck = Math.min(allFields.size(), 50 - defaultFieldCount);
        for (int i = 0; i < sizeToCheck; i++) {
            checkCustomFieldByIndex(i + defaultFieldCount, allFields.get(i));
        }
    }

    public void checkSortingOfAllFields(List<String> names) {
        final int sizeToCheck = Math.min(names.size(), 50);
        waitForElements(fieldNameValues);
        wait.until(
                ExpectedConditions.numberOfElementsToBe(
                        By.cssSelector("[data-testid='field-name']"), sizeToCheck));
        for (int i = 0; i < sizeToCheck; i++) {
            scrollToElement(fieldNameValues.get(i));
            assertEquals(fieldNameValues.get(i).getText(), names.get(i));
        }
    }

    public void checkActionsColumnIsNotDisplayed() {
        isElementInvisible(actionsColumn);
    }

    public void checkActionsColumnIsDisplayed() {
        isElementEnabled(actionsColumn);
    }

    public void changeSorting() {
        click(nameSortElement);
    }

    public void searchForField(String name) {
        type(searchInput, name);
    }

    public void checkEmptySearchResult(String searchedName) {
        final String emptyPageText = "No results for \"" + searchedName;
        checkText(emptySearchResult, emptyPageText);
    }

    public void openCreateFieldPopup() {
        click(createNewFieldButton);
    }

    public void selectType(JSONObject fieldBody) {
        waitForElement(createNewFieldPopup);
        click(typeDropdown);
        final WebElement dropdownElement =
                driver.findElement(
                        By.cssSelector("[data-testid='" + fieldBody.getString("type") + "-select-option']"));
        click(dropdownElement);
    }

    public void fillCreateFieldPopupFields(JSONObject fieldBody) {
        waitForElement(createNewFieldPopup);
        type(internalNameField, fieldBody.getString(INTERNAL_NAME));
        click(createNewFieldPopupHeader);
        checkValue(displayNameField, fieldBody.getString(INTERNAL_NAME));

        type(displayNameField, fieldBody.getString(DISPLAY_NAME));
        if (fieldBody.has(OPTIONS)) {
            final JSONArray options = fieldBody.getJSONArray(OPTIONS);
            final int minimumOptionsRequired =
                    fieldBody.getString("type").equals(CHECKBOX.name()) ? 1 : 2;
            for (int i = 0; i < minimumOptionsRequired; i++) {
                type(fieldOptionNames.get(i), options.getJSONObject(i).getString(INTERNAL_NAME));
            }
            for (int i = minimumOptionsRequired; i < options.length(); i++) {
                click(addOptionButton);
                type(fieldOptionNames.get(i), options.getJSONObject(i).getString(INTERNAL_NAME));
            }
        }
    }

    public void createNewField() {
        click(submitButtonOnPopup);
    }

    public void clickOnSaveButton() {
        click(submitButtonOnPopup);
    }

    public void closeFieldPopupByCancel() {
        click(cancelButton);
    }

    public void closeFieldPopup() {
        click(popUpCloseButton);
    }

    public void clickOnAddOptionButton() {
        click(addOptionButton);
    }

    public void checkCreateSuccessToast() {
        final String text = "The field has been created successfully.";
        checkText(successToastMessage, text);
    }

    public void checkUpdateSuccessToast() {
        final String text = "The field has been updated successfully.";
        checkText(successToastMessage, text);
    }

    public void checkDeleteSuccessToast() {
        final String text = "The field has been successfully deleted";
        checkText(successToastMessage, text);
    }

    public void checkFieldAlreadyExistsErrorToast() {
        final String text = "Field with this name already exists";
        checkText(dangerToastMessage, text);
    }

    public void checkTrashIconIsDisabledAtRow(int orderOfIcon) {
        waitForElements(trashIconElements);
        isElementDisabled(trashIconElements.get(orderOfIcon));
    }

    public void checkTrashIconIsEnabled(int orderOfIcon) {
        isElementEnabled(trashIconElements.get(orderOfIcon));
    }

    public void checkAddOptionButtonIsDisabled() {
        isElementDisabled(addOptionButton);
    }

    public void checkRequiredFieldErrors() {
        final String requiredFieldErrorMessage = "This field is required";
        checkText(internalNameRequiredError, requiredFieldErrorMessage);
        checkText(displayNameRequiredError, requiredFieldErrorMessage);
        for (int i = 0; i < optionNameError.size(); i++) {
            checkText(optionNameError.get(i), requiredFieldErrorMessage);
        }
    }

    public void checkRequiredFields() {
        type(internalNamePopupElement, "");
        type(displayNamePopupElement, "");
        click(submitButtonOnPopup);
        checkText(internalNameRequiredError, REQUIRED_MESSAGE);
        checkText(displayNameRequiredError, REQUIRED_MESSAGE);
    }

    public void duplicateOptionsErrorCheck() {
        final String requiredFieldErrorMessage = "Option with this name already exists";
        for (int i = 0; i < optionNameError.size(); i++) {
            checkText(optionNameError.get(i), requiredFieldErrorMessage);
        }
    }

    public void checkRequiredFieldsInEditFieldPopup() {
        clearText(internalNamePopupElement);
        clearText(displayNamePopupElement);
        clickOnAddOptionButton();
        clickOnSaveButton();
        checkRequiredFieldErrors();
    }

    public void checkCreateButtonIsNotDisplayed() {
        isElementInvisible(createNewFieldButton);
    }

    public void checkNoToastMessageIsDisplayed() {
        isElementInvisible(successToastMessage);
    }

    public void checkPopupIsClosed() {
        isElementInvisible(createNewFieldPopup);
    }

    public void checkOptionText(int indexOfOption, String expectedValue) {
        checkValue(fieldOptionNames.get(indexOfOption), expectedValue);
    }

    public void openFieldEditPopup(String name) {
        waitForElements(fieldNameValues);
        click(driver.findElement(By.cssSelector("[title='" + name + "']")));
    }

    public void checkFieldDetails(JSONObject field) {
        checkText(fieldTypePopupElement, FieldTypes.valueOf(field.getString("type")).getDisplayName());
        checkText(displayNamePopupElement, field.getString("nameTranslation"));
        checkText(internalNamePopupElement, field.getString("internalName"));
        if (field.has("options")) {
            checkOptionNames(field.getJSONArray("options"));
        }
    }

    private void checkOptionNames(JSONArray optionNames) {
        for (int i = 0; i < optionNames.length(); i++) {
            checkText(
                    optionNamePopupElements.get(i), optionNames.getJSONObject(i).getString("internalName"));
        }
    }

    public void checkFirstSevenActionsAreDisabled() {
        waitForElements(disabledActionFieldElements);
        Assert.assertEquals(disabledActionFieldElements.size(), 7);
    }

    public void fillFieldsInEditFieldPopup(JSONObject editField) {
        type(internalNamePopupElement, editField.getString(INTERNAL_NAME));
        type(displayNamePopupElement, editField.getString(DISPLAY_NAME));
    }

    public void checkDisplayNameAutoFill(String displayName) {
        type(displayNamePopupElement, " ");
        clickOut();
        checkText(displayNamePopupElement, displayName);
    }

    public void clickOnActionMenuButton(int index) {
        waitForElements(actionMenuElements);
        click(actionMenuElements.get(index));
    }

    public void checkWarningToastMessage() {
        checkText(warningToastElement, "Warning!\n" +
                "Deleting this field will affect all services using it. The field will be deleted permanently. Are you sure you want to proceed?\n" +
                "Delete\n" +
                "Cancel");
    }

    public void clickOnDeleteButtonFromActionMenuList() {
        click(fieldItemDeleteButtonElement);
    }

    public void clickOnDeleteButton() {
        click(deleteButtonElement);
    }

    public void clickOnCancelButtonElement() {
        click(cancelButtonToastElement);
    }

    public void checkWarningToastNotDisplayed() {
        isElementInvisible(warningToastElement);
    }

    public void checkActionsMenuNotDisplayed() {
        isElementInvisible(actionMenuListElement);
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
