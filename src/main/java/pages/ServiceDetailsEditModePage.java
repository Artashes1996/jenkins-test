package pages;

import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import helpers.flows.ServiceFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import utils.TestUtils;

import java.util.List;
import java.util.stream.Collectors;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.*;

public class ServiceDetailsEditModePage extends BasePage<ServiceDetailsEditModePage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/services/";

    public String serviceId;

    @FindBy(css = "[id='SERVICE_FORM']")
    private WebElement serviceForm;

    @FindBy(css = "[data-testid='select-control']")
    private WebElement selectControl;

    @FindBy(css = "[data-testid='service-internal-name']")
    private WebElement serviceName;

    @FindBy(css = "[data-testid='service-display-name']")
    private WebElement displayName;

    @FindBy(css = "[data-testid='service-display-name-label'] label")
    private WebElement displayNameLabel;

    @FindBy(css = "[data-testid='resource-selection']")
    private WebElement resourceSelectionLabelElement;

    @FindBy(css = "[data-testid='resource-selection-menu-list']")
    private WebElement resourceSelectionOptionsContainer;

    @FindBy(css = "[id*='option']")
    private List<WebElement> resourceSelectionOptions;

    @FindBy(css = "[data-testid='service-status-label'] label")
    private WebElement statusElement;

    @FindBy(css = "[data-testid='service-duration-error']")
    private WebElement serviceDurationError;

    @FindBy(css = "[data-testid='service-duration']")
    private WebElement serviceDuration;

    //TODO feedback for FEDs to provide data-testid
    @FindBy(xpath = "//input[@data-testid='service-visibility']/parent::label")
    private WebElement visibilityElement;

    @FindBy(css = "[data-testid='field-name']")
    private List<WebElement> fieldNameLists;

    @FindBy(css = "[data-testid='field-type']")
    private List<WebElement> fieldTypeLists;

    @FindBy(css = "[data-testid='field-kiosk-hidden'] input")
    private List<WebElement> visibilityCheckBox;

    @FindBy(css = "[data-testid='visibility-web-kiosk']")
    private WebElement webKiosk;

    @FindBy(css = "[data-testid='visibility-physical-kiosk']")
    private WebElement physicalKiosk;

    @FindBy(css = "[data-testid='visibility-monitor']")
    private WebElement monitor;

    @FindBy(css = "[class*='visibility-checkbox'] label")
    private List<WebElement> visibilityLabels;

    @FindBy(css = "[data-testid='field-required'] input")
    private List<WebElement> fieldRequiredLists;

    @FindBy(css = "[data-testid='add-field-button']")
    private WebElement addFieldButton;

    @FindBy(css = "[data-testid='field-action-popup'] label")
    private List<WebElement> addFieldCheckboxes;

    @FindBy(css = "[data-testid='field-name-popup']")
    private List<WebElement> fieldNamesfromPopup;

    @FindBy(css = "[data-testid='field-type-popup']")
    private List<WebElement> fieldTypes;

    @FindBy(css = "[data-testid='submit']")
    private WebElement submit;

    @FindBy(css = "[data-testid='action-delete']")
    private List<WebElement> actionsDelete;

    @FindBy(css = "[data-testid='service-create']")
    private WebElement serviceEditSave;

    @FindBy(css = "[data-testid='confirm-update']")
    private WebElement confirmUpdate;

    @FindBy(css = "[data-testid='service-cancel']")
    private WebElement cancelButton;

    @FindBy(css = "[data-testid='cancel']")
    private WebElement cancelAddingFields;

    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResult;

    @FindBy(css = "[data-testid='table-actions']")
    private List<WebElement> tableActions;

    private final ServiceFlows serviceflow = new ServiceFlows();

    public ServiceDetailsEditModePage(String browser, String version, String organizationId, String serviceId, String token) {
        super(browser, version, organizationId, token);
        this.serviceId = serviceId;
    }

    public ServiceDetailsEditModePage(String browser, String version, String serviceId, String token) {
        super(browser, version, token);
        this.serviceId = serviceId;
    }

    public void checkAddedFields(JSONArray fieldsToAdd) {
        fieldsToAdd.forEach(field -> {
            final JSONObject fieldJson = (JSONObject) field;
            final List<String> fieldsNamesText = fieldNameLists.stream().map(WebElement::getText).collect(Collectors.toList());
            final int fieldIndex = fieldsNamesText.indexOf(fieldJson.getString("internalName"));
            checkText(fieldTypeLists.get(fieldIndex), FieldTypes.valueOf(fieldJson.getString("type")).getDisplayName());
            isElementNotSelected(visibilityCheckBox.get(fieldIndex));
            isElementNotSelected(fieldRequiredLists.get(fieldIndex));
        });
    }

    public void addFieldFromPopup(JSONArray fieldsToAdd) {
        click(addFieldButton);
        waitForElements(addFieldCheckboxes);
        fieldsToAdd.forEach(field -> {
            for (int i = 0; i < fieldNamesfromPopup.size(); i++) {
                if (fieldNamesfromPopup.get(i).getText().equals(((JSONObject) field).getString("internalName"))) {
                    click(addFieldCheckboxes.get(i));
                    break;
                }
            }
        });
        click(submit);
        checkAddedFields(fieldsToAdd);
        saveChanges();
    }


    public void changeAllSettings(JSONObject service) {
        type(serviceName, service.getString("internalName"));

        cleanText(displayName);
        clickOut();
        checkValue(displayName, service.getString("internalName"));

        click(selectControl);
        final String resourceSelection = TestUtils.capitalize(service.getString("resourceSelection"));
        waitForElements(resourceSelectionOptions);

        resourceSelectionOptions.stream().filter(option -> option.getText().equals(resourceSelection)).findFirst().orElseThrow().click();
        checkText(selectControl, resourceSelection);

        cleanText(serviceDuration);
        type(serviceDuration, serviceflow.getServiceDurationInMinutes(service));

        click(statusElement);
        checkText(statusElement, TestUtils.capitalize(ServiceUpdateRequestBody.Status.INACTIVE.name()));

        saveChanges();
    }

    public void changeVisibility(JSONObject service) {
        click(visibilityElement);
        if (serviceflow.isServiceVisible(service)) {
            checkText(visibilityElement, "Hidden");
            visibilityLabels.forEach(this::isElementInvisible);
        } else {
            checkText(visibilityElement, "Visible on");
            visibilityCheckBox.forEach(this::checkElementIsSelected);
            click(visibilityLabels.get(0));
            click(visibilityLabels.get(1));
            checkText(visibilityElement, "Visible on");
        }

        saveChanges();
    }

    public void checkAllSettings(JSONObject service) {
        checkValue(displayName, service.getString("nameTranslation"));
        checkValue(serviceName, service.getString("internalName"));

        checkText(selectControl, TestUtils.capitalize(service.getString("resourceSelection")));
        checkText(statusElement, TestUtils.capitalize(service.getString("status")));

        final int serviceDurationValue = (int) Math.ceil(service.getFloat("duration") / 60);
        checkValue(serviceDuration, String.valueOf(serviceDurationValue));

        saveChanges();
    }

    public void checkVisibility(JSONObject service) {
        if (serviceflow.isServiceVisible(service)) {
            visibilityLabels.forEach(this::isElementEnabled);
        } else {
            checkText(visibilityElement, "Hidden");
            visibilityLabels.forEach(this::isElementInvisible);
        }
        saveChanges();
    }

    public void checkFields(JSONObject service) {
        for (int i = 0; i < service.getJSONArray("fieldLinks").length(); i++) {
            final JSONObject field = service.getJSONArray("fieldLinks").getJSONObject(i);
            checkIsFieldsCorerrect(i, field);
        }
        saveChanges();
    }

    public void checkIsFieldsCorerrect(int i, JSONObject field) {
        checkText(fieldNameLists.get(i), field.getString("fieldInternalName"));

        if (field.getString("fieldType").equals(FieldTypes.MULTI_SELECT_DROPDOWN.name()) ||
                field.getString("fieldType").equals(FieldTypes.SINGLE_SELECT_DROPDOWN.name())) {
            checkText(fieldTypeLists.get(i), "Dropdown");
        } else {
            checkText(fieldTypeLists.get(i), TestUtils.capitalize(field.getString("fieldType")));
        }

        if (field.getString("displayTo").equals("EVERYONE")) {
            checkElementIsNotSelected(visibilityCheckBox.get(i));
        } else {
            checkElementIsSelected(visibilityCheckBox.get(i));
        }

        if (field.getBoolean("optional")) {
            checkElementIsNotSelected(fieldRequiredLists.get(i));
        } else {
            checkElementIsSelected(fieldRequiredLists.get(i));
        }
    }

    public void checkDurationError() {
        final String valueBelowAcceptable = "4";
        cleanText(serviceDuration);
        type(serviceDuration, valueBelowAcceptable);
        clickOut();
        checkText(serviceDurationError, "Minimum duration can not be less than 5 minutes.");
    }

    public void checkDurationRequiredFieldError() {
        cleanText(serviceDuration);
        clickOut();
        checkText(serviceDurationError, "This field is required.");
    }

    public void cancelChanges() {
        click(statusElement);
        checkText(statusElement, TestUtils.capitalize(ServiceUpdateRequestBody.Status.INACTIVE.name()));
        click(cancelButton);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("edit")));
    }

    public void saveChanges() {
        click(serviceEditSave);
        click(confirmUpdate);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("edit")));
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + serviceId + "/details/edit";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), getPageUrl());
        waitForElement(serviceForm);
    }
}
