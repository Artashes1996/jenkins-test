package pages;

import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import lombok.Getter;
import org.json.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import java.util.*;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.DisplayTo.STAFF_ONLY;
import static org.testng.Assert.assertEquals;
import static utils.TestUtils.capitalize;

public class CreateServicePage extends BasePage<CreateServicePage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/services/create";

    @FindBy(css = "[data-testid='service-internal-name']")
    private WebElement serviceNameElement;

    @FindBy(css = "[data-testid='service-internal-name-error']")
    private WebElement serviceNameErrorMessageElement;

    @FindBy(css = "[data-testid='service-display-name']")
    private WebElement displayNameElement;

    @FindBy(css = "[data-testid='service-display-name-error']")
    private WebElement displayNameErrorMessageElement;

    @FindBy(css = "[data-testid='select-control']")
    private WebElement resourceSelectionElement;

    @FindBy(xpath = "//*[@data-testid='service-status']/parent::label")
    private WebElement statusElement;

    @FindBy(css = "[data-testid='service-duration']")
    private WebElement serviceDurationElement;

    @FindBy(css = "[data-testid='service-duration-error']")
    private WebElement serviceDurationErrorMessageElement;

    @FindBy(xpath = "//*[@data-testid='service-visibility']/parent::label")
    private WebElement visibilityElement;

    @FindBy(xpath = "//*[@data-testid='visibility-web-kiosk']/parent::label")
    private WebElement webKioskCheckBoxElement;

    @FindBy(css = "[data-testid='visibility-web-kiosk']")
    private WebElement webKioskElement;

    @FindBy(xpath = "//*[@data-testid='visibility-physical-kiosk']/parent::label")
    private WebElement physicalKioskCheckBoxElement;

    @FindBy(css = "[data-testid='visibility-physical-kiosk']")
    private WebElement physicalKioskElement;

    @FindBy(xpath = "//*[@data-testid='visibility-monitor']/parent::label")
    private WebElement monitorCheckBoxElement;

    @FindBy(css = "[data-testid='visibility-monitor']")
    private WebElement monitorElement;

    @FindBy(css = "[data-testid='service-cancel']")
    private WebElement cancelButtonElement;

    @FindBy(css = "[data-testid='service-create']")
    private WebElement createButtonElement;

    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement dangerToastElement;

    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToast;

//  TODO change selector
    @FindBy(className = "ql-select__menu")
    private WebElement resourceSelectionDropDownElement;

    @FindBy(css = "[data-testid='field-name']")
    private List<WebElement> fieldNameElements;

    @FindBy(css = "[data-testid='field-type']")
    private List<WebElement> fieldTypeElements;

    @FindBy(css = "[data-testid='field-kiosk-hidden'] label")
    private List<WebElement> checkboxHiddenOnKiosk;

    @FindBy(css = "[data-testid='field-required'] label")
    private List<WebElement> checkboxRequired;

    @FindBy(css = "[data-testid='action-delete']")
    private List<WebElement> deleteActionElements;

    @FindBy(css = "[data-testid='action-moove-up']")
    private List<WebElement> moveActionUpElements;

    @FindBy(css = "[data-testid='action-moove-down']")
    private List<WebElement> moveActionDownElements;

    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResultElement;

    @FindBy(css = "[data-testid='input-search']")
    private WebElement searchFieldElement;

    @FindBy(css = "[data-testid='add-field-button']")
    private WebElement addFieldButtonElement;

    @FindBy(css = "[data-testid='field-name-popup']")
    private List<WebElement> fieldNamePopupElements;

    @FindBy(css = "[data-testid='field-type-popup']")
    private List<WebElement> fieldTypePopupElements;

    @FindBy(css = "[data-testid='field-action-popup'] label")
    private List<WebElement> fieldActionPopupElements;

    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancelFieldsPopupElement;

    @FindBy(css = "[data-testid='submit']")
    private WebElement addFieldsButtonElement;

    @FindBy(css = "[data-testid=' table-loading']")
    private WebElement tableLoadingElement;

    @FindBy(css = "[data-testid='popup-content']")
    private WebElement popupContentElement;

    @FindBy(css = "[data-testid='popup-content-container']")
    private WebElement popupContentContainerElement;


    public enum ResourceSelectionType {
        DISABLED,
        ALLOWED,
        REQUIRED
    }

    public enum Tabs {
        PERSONAL_DETAILS(
                By.xpath("//*[contains(@class, '_tabs_1eiio_1')]/button[contains(text(), 'Personal')]")),
        LOCATIONS(
                By.xpath("//*[contains(@class, '_tabs_1eiio_1')]/button[contains(text(), 'locations')]")),
        SERVICES(
                By.xpath("//*[contains(@class, '_tabs_1eiio_1')]/button[contains(text(), 'services')]"));

        private By by;

        Tabs(By by) {
            this.by = by;
        }

        public By getBy() {
            return by;
        }
    }

    public enum Visibility {
        VISIBILITY_ON("Visible on"),
        HIDDEN("Hidden");

        @Getter
        private final String state;

        Visibility(String state) {
            this.state = state;
        }
    }

    public void selectResourceSelection(ResourceSelectionType type) {
        click(resourceSelectionElement);
        waitForElement(resourceSelectionDropDownElement);
        final WebElement selectMenu = driver
                .findElement(By.cssSelector("[data-testid='" + type + "-select-option']"));
        click(selectMenu);
    }

    public CreateServicePage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public CreateServicePage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public void fillServiceFields(
            String serviceName,
            String displayName,
            String durationValue,
            ResourceSelectionType resourceSelectionType) {
        type(serviceNameElement, serviceName);
        type(serviceNameElement, displayName);
        type(serviceDurationElement, durationValue);
        selectResourceSelection(resourceSelectionType);
    }

    public void fillValueInServiceNameField(String value) {
        type(serviceNameElement, value);
    }

    public void fillValueInServiceDurationField(String number) {
        type(serviceDurationElement, number);
    }

    public void clearServiceDurationValue() {
        serviceDurationElement.clear();
    }

    public void clickOnCancelButton() {
        click(cancelButtonElement);
    }

    public void clickOnCreateButton() {
        click(createButtonElement);
    }

    public void clickOnStatusToggle() {
        click(statusElement);
    }

    public void clickOnVisibilityToggle() {
        click(visibilityElement);
    }

    public void checkRequiredErrorMessageForServiceNameField() {
        final String errorMsg = "This field is required.";
        checkText(serviceNameErrorMessageElement, errorMsg);
    }

    public void checkRequiredErrorMessageForDisplayNameField() {
        final String errorMsg = "This field is required.";
        checkText(displayNameErrorMessageElement, errorMsg);
    }

    public void checkRequiredErrorMessageForServiceDurationField() {
        final String errorMsg = "This field is required.";
        checkText(serviceDurationErrorMessageElement, errorMsg);
    }

    public void checkServiceDurationCannotBeLessThanFiveMinutes() {
        final String errorMsg = "Minimum duration can not be less than 5 minutes.";
        checkText(serviceDurationErrorMessageElement, errorMsg);
    }

    public void checkExistingServiceErrorMsg() {
        final String errorMsg = "Sorry\n" + "Service with this name already exists.";
        checkText(dangerToastElement, errorMsg);
    }

    public void checkSuccessToast() {
        final String successMessage = "Success\n" +
                "The service has been created successfully.";
        checkText(successToast, successMessage);
    }

    public void clearDisplayNameFieldValue() {
        displayNameElement.clear();
    }

    public void clearServiceNameFieldValue() {
        serviceNameElement.clear();
    }

    public void checkDisplayNameFieldValue(String value) {
        checkValue(displayNameElement, value);
    }

    public void checkCheckBoxesNotDisplayed() {
        isElementInvisible(webKioskElement);
        isElementInvisible(physicalKioskElement);
        isElementInvisible(monitorElement);
    }

    public void checkVisibilityState(Visibility visibility) {
        checkText(visibilityElement, visibility.state);
    }

    public void checkWebKioskCheckbox() {
        click(webKioskCheckBoxElement);
    }

    public void checkPhysicalKioskCheckbox() {
        click(physicalKioskCheckBoxElement);
    }

    public void checkMonitorCheckbox() {
        click(monitorCheckBoxElement);
    }

    public void checkCheckboxesAreSelected() {
        checkElementIsSelected(webKioskElement);
        checkElementIsSelected(physicalKioskElement);
        checkElementIsSelected(monitorElement);
    }

    public void checkSelectedResourceSelection(String value) {
        assertEquals(resourceSelectionElement.getText(), value);
    }

    public void checkStatusState(String value) {
        assertEquals(statusElement.getText(), value);
    }

    public void checkServiceDurationValue(String value) {
        assertEquals(serviceDurationElement.getAttribute("value"), value);
    }

    public void checkFieldsData(JSONArray fields) {
        assertEquals(fieldNameElements.size(), fields.length());
        for (int i = 0; i < fields.length(); i++) {
            final JSONObject field = fields.getJSONObject(i);
            checkText(fieldNameElements.get(i), field.getString("internalName"));
            String fieldType = field.getString("type");
            if (fieldType.equals(FieldTypes.MULTI_SELECT_DROPDOWN.name()) ||
                    fieldType.equals(FieldTypes.SINGLE_SELECT_DROPDOWN.name())) {
                fieldType = "DROPDOWN";
            }
            checkText(fieldTypeElements.get(i), capitalize(fieldType));
            if (field.has("displayTo") && field.getString("displayTo").equals(STAFF_ONLY.name())) {
                checkElementIsSelected(checkboxHiddenOnKiosk.get(i));
            } else {
                checkElementIsNotSelected(checkboxHiddenOnKiosk.get(i));
            }
            if (field.has("optional") && field.getBoolean("optional")) {
                checkElementIsSelected(checkboxRequired.get(i));
            } else {
                checkElementIsNotSelected(checkboxRequired.get(i));
            }
        }
    }

    public void deleteAllFields() {
        waitForElements(fieldNameElements);
        final int size = fieldNameElements.size();
        for (int fieldsCount = 0; fieldsCount < size; fieldsCount++) {
            deleteField(0);
            Assert.assertEquals(fieldNameElements.size(), size - fieldsCount - 1);
        }
    }

    public void deleteField(int index) {
        hoverOnElement(fieldNameElements.get(index));
        deleteActionElements.get(index).click();
    }

    public void checkHiddenOnKiosk(int index) {
        click(checkboxHiddenOnKiosk.get(index));
    }

    public void checkRequired(int index) {
        click(checkboxRequired.get(index));
    }

    public void checkEmptyResult() {
        checkText(emptyResultElement, "Sorry :(\n" + "There are no fields");
    }

    public void checkFieldsDataInAddFieldPopup(List<JSONObject> notSelectedFields) {
        waitForElements(fieldActionPopupElements);
        assertEquals(notSelectedFields.size(), fieldActionPopupElements.size());
        for (int i = 0; i < notSelectedFields.size(); i++) {
            final JSONObject field = notSelectedFields.get(i);
            checkFieldDataInAddFieldsPopup(i, field);
            isElementNotSelected(fieldActionPopupElements.get(i));
        }
    }

    public void checkFieldDataInAddFieldsPopup(int index, JSONObject field) {
        checkText(fieldNamePopupElements.get(index), field.getString("internalName"));
        String fieldType = field.getString("type");
        if (fieldType.equals(FieldTypes.MULTI_SELECT_DROPDOWN.name()) || fieldType.equals(FieldTypes.SINGLE_SELECT_DROPDOWN.name())) {
            fieldType = "DROPDOWN";
        }
        checkText(fieldTypePopupElements.get(index), capitalize(fieldType));
    }

    public void clickOnAddFieldButton() {
        click(addFieldButtonElement);
    }

    public void clickOnAddFieldsButtonFromPopup() {
        click(addFieldsButtonElement);
    }

    public void clickOnCancelButtonFromFieldsPopup() {
        click(cancelFieldsPopupElement);
    }

    public void checkFieldsPopupIsNotDisplayed() {
        isElementInvisible(popupContentContainerElement);
    }

    public void checkEmptySearchResult() {
        waitForElementIsNotVisible(tableLoadingElement);
        waitForElements(fieldNamePopupElements);
        final String stringToSearch = UUID.randomUUID().toString();
        type(searchFieldElement, stringToSearch);
        checkText(emptyResultElement, "No results for \"" + stringToSearch);
    }

    public void addAllFieldsExistingInPopup() {
        waitForElementIsNotVisible(tableLoadingElement);
        waitForElements(fieldActionPopupElements);
        final int fieldsCount = fieldActionPopupElements.size();
        for (int i = 0; i < fieldsCount; i++) {
            selectFieldFromAddFieldPopup(i);
        }
        checkText(addFieldsButtonElement, "ADD " + fieldsCount + " FIELDS");
        clickOnAddFieldsButtonFromPopup();
    }

    public void searchField(String value) {
        waitForElements(fieldNamePopupElements);
        type(searchFieldElement, value);
    }

    public void selectFieldFromAddFieldPopup(int index) {
        waitForElements(fieldActionPopupElements);
        click(fieldActionPopupElements.get(index));
    }

    public void checkFieldNotPresentInPopup(JSONObject fieldObject) {
        waitForElement(popupContentElement);
        checkElementNotPresent(By.cssSelector("[data-testid='field-name-popup'][title='"
                + fieldObject.getString("internalName") + "']"));
    }

    public void checkAddFieldsButtonIsDisabled() {
        isElementDisabled(addFieldsButtonElement);
    }

    public void checkAddFieldsButtonText(int count) {
        checkText(addFieldsButtonElement, "ADD " + count + " FIELDS");
    }

    public void arrowUpButtonIsDisabled(int index) {
        hoverOnElement(fieldNameElements.get(index));
        isElementDisabled(moveActionUpElements.get(index));
    }

    public void arrowDownButtonIsDisabled(int index) {
        hoverOnElement(fieldNameElements.get(index));
        isElementDisabled(moveActionDownElements.get(index));
    }

    public void moveUp(int index)  {
        hoverOnElement(fieldNameElements.get(index));
        click(moveActionUpElements.get(index));
    }

    public void moveDown(int index)  {
        hoverOnElement(fieldNameElements.get(index));
        click(moveActionDownElements.get(index));
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
        waitForElement(visibilityElement);
    }
}
