package pages;

import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.flows.ServiceFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.TestUtils;
import java.util.List;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody.*;
import static org.testng.Assert.*;

public class ServiceDetailsViewModePage extends BasePage<ServiceDetailsViewModePage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/services/";

    public String serviceId;

    @FindBy(css = "[data-testid='service-title']")
    private WebElement serviceTitleElement;

    @FindBy(css = "[data-testid='service-name']")
    private WebElement serviceNameLabelElement;

    @FindBy(css = "[data-testid='display-name']")
    private WebElement displayNameLabelElement;

    @FindBy(css = "[data-testid='resource-selection']")
    private WebElement resourceSelectionLabelElement;

    @FindBy(css = "[data-testid='status']")
    private WebElement statusLabelElement;

    @FindBy(css = "[data-testid='duration']")
    private WebElement durationLabelElement;

    @FindBy(css = "[data-testid='visibility']")
    private WebElement visibilityLabelElement;

    @FindBy(css = "[data-testid^='breadcrumb-item']")
    private List<WebElement> breadCrumbItems;

    @FindBy(css = "[data-testid='visibility']")
    private WebElement visibilityElement;

    @FindBy(css = "[data-testid='field-name']")
    private List<WebElement> fieldNameLists;

    @FindBy(css = "[data-testid='field-type']")
    private List<WebElement> fieldTypeLIsts;

    @FindBy(css = "[data-testid=success-toast]")
    private WebElement successToast;

    @FindBy(css = "[data-testid='field-kiosk-hidden'] input")
    private List<WebElement> visibilityCheckBox;

    @FindBy(css = "[data-testid='field-required'] input")
    private List<WebElement> fieldRequiredLists;

    private final ServiceFlows serviceFlows = new ServiceFlows();

    public void checkDisplayNameFieldValue(String value) {
        checkText(displayNameLabelElement, value);
    }

    public ServiceDetailsViewModePage(String browser, String version, String organizationId, String serviceId, String token) {
        super(browser, version, organizationId, token);
        this.serviceId = serviceId;
    }

    public ServiceDetailsViewModePage(String browser, String version, String serviceId, String token) {
        super(browser, version, token);
        this.serviceId = serviceId;
    }

    public ServiceDetailsViewModePage(String browser, String version, String serviceId) {
        super(browser, version);
        this.serviceId=serviceId;
    }

    private String visibilityValue(JSONObject service){
        final JSONObject visibility = service.getJSONObject("visibility");
        final String webKiosk = visibility.getBoolean(WEB_KIOSK)?"Web Kiosk\n":"";
        final String phisicalKiosk = visibility.getBoolean(PHYSICAL_KIOSK)?"Physical Kiosk\n":"";
        final String monitor = visibility.getBoolean(MONITOR)?"Monitor":"";
        final String result = webKiosk + phisicalKiosk + monitor;
        return result.isEmpty()?"Hidden":result;
    }

    public void checkServiceDetails(JSONObject service) {
        checkText(serviceTitleElement, service.getString("internalName"));
        checkText(serviceNameLabelElement, service.getString("internalName"));
        final String resourceSelection = TestUtils.capitalize(service.getString("resourceSelection"));
        checkText(resourceSelectionLabelElement, resourceSelection);
        checkText(displayNameLabelElement, service.getString("nameTranslation"));
        final String status = TestUtils.capitalize(service.getString("status"));
        checkText(statusLabelElement, status);

        checkText(visibilityLabelElement, visibilityValue(service));
        checkText(durationLabelElement,serviceFlows.getServiceDurationInMinutes(service)+ " Minutes");

        if (fieldNameLists.size() > 0) {
            final JSONArray fields = service.getJSONArray("fieldLinks");
            for (int i = 0; i < fields.length(); i++) {
                final JSONObject field = fields.getJSONObject(i);
                checkText(fieldNameLists.get(i), field.getString("fieldInternalName"));
                if (field.getString("fieldType").equals(FieldTypes.MULTI_SELECT_DROPDOWN.name()) ||
                        field.getString("fieldType").equals(FieldTypes.SINGLE_SELECT_DROPDOWN.name())) {
                    checkText(fieldTypeLIsts.get(i), "Dropdown");
                } else {
                    checkText(fieldTypeLIsts.get(i), TestUtils.capitalize(field.getString("fieldType")));
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
        }
    }

    public void checkToast(){
        final String toastPass = "Success\nThe service has been updated successfully.";
        checkText(successToast, toastPass);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + serviceId + "/details";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    public void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), getPageUrl());
        waitForElement(visibilityLabelElement);
    }
    public void checkBreadcrumb(JSONObject service) {
        assertEquals(breadCrumbItems.size(), 2);
        checkText(breadCrumbItems.get(0), "Services");
        checkText(breadCrumbItems.get(1), service.getString("internalName"));
        click(breadCrumbItems.get(0));
        final String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("services") && !currentUrl.contains(serviceId));
    }

}
