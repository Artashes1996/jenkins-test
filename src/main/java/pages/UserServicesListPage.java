package pages;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.commons.ToggleAction;

import java.util.*;

import static org.testng.Assert.*;
import static utils.TestUtils.*;

import static configuration.Config.UI_URI_CONSOLE;

public class UserServicesListPage extends BasePage<UserServicesListPage> {
    @FindBy(css = "[data-testid='avatar']")
    private WebElement avatarElement;
    @FindBy(css = "[data-testid='user-full-name']")
    private WebElement fullNameElement;
    @FindBy(css = "[data-testid='user-role']")
    private WebElement userRoleElement;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationContentElement;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement paginationPrevButtonElement;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement paginationNextElement;
    @FindBy(css = "[data-testid='bar-title']")
    private WebElement servicesCountElement;

    @FindBy(css = "[data-testid='user-location']")
    private WebElement locationDropdownContent;
    @FindBy(css = "[data-testid='select-control']")
    private WebElement locationDropdown;
    @FindBy(css = "[class='ql-select__input']")
    private WebElement locationSearchInput;
    @FindBy(css = "[data-testid='user-location-menu-list']")
    private WebElement locationDropdownContainer;
    @FindBy(css = "[class*='_empty-text']")
    private WebElement locationDropdownEmptySearchElement;

    @FindBy(css = "[data-testid*='-select-option']")
    private List<WebElement> locationsOptions;
    @FindBy(css = "[data-testid='input-search']")
    private WebElement serviceSearchElement;
    @FindBy(css = "[data-testid='service-name']")
    private List<WebElement> servicesNamesElements;
    @FindBy(css = "td[data-testid='service-visibility']")
    private List<WebElement> servicesVisibilityElements;
    @FindBy(css = "[data-testid='service-duration']")
    private List<WebElement> servicesDurationElements;
    @FindBy(css = "[data-testid='service-status']")
    private List<WebElement> servicesStatusElements;
    @FindBy(css = "[data-testid='actions-header']")
    private WebElement actionsHeader;
    @FindBy(css = "[data-testid='actions-link-unlink']")
    private List<WebElement> servicesLinkUnlinkElements;
    @FindBy(css = "[data-testid='service-name-header']")
    private WebElement servicesNameHeader;
    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResultElement;
    @FindBy(css = "[class*='select--is-disabled']")
    private WebElement locationDropDownDisabled;
    @FindBy(css = "[class^='_table-container']")
    private WebElement tableContainer;


    private static final String PAGE_URL = UI_URI_CONSOLE + "/company/management/user/";

    public UserServicesListPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public UserServicesListPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public UserServicesListPage(String browser, String version) {
        super(browser, version);
    }

    public void checkUserDetails(JSONObject user) {
        final String userInitials = user.getString("firstName").charAt(0) + user.getString("lastName").substring(0, 1);
        checkText(avatarElement, userInitials);
        final String userFullName = user.getString("firstName") + " " + user.getString("lastName");
        checkText(fullNameElement, userFullName);
        final String role = capitalize(user.getString("role"));
        assertEquals(userRoleElement.getText(), role);
    }

    public void checkServicesInPageEditAccess(List<JSONObject> linkedServices, List<JSONObject> notLinkedServices) {
        for (int i = 0; i < linkedServices.size(); i++) {
            checkService(i, linkedServices.get(i));
            checkServiceActionToBe(i, ToggleAction.UNLINK);
        }
        for (int i = 0; i < notLinkedServices.size(); i++) {
            int notLinkedServicesRow = i + linkedServices.size();
            checkService(notLinkedServicesRow, notLinkedServices.get(i));
            checkServiceActionToBe(notLinkedServicesRow, ToggleAction.LINK);
        }
    }

    public void checkServicesInPageViewAccess(List<JSONObject> servicesList) {
        for (int i = 0; i < servicesList.size(); i++) {
            checkService(i, servicesList.get(i));
        }
        checkLinkUnlinkNotPresent();
    }

    public void searchForService(JSONObject service) {
        final String serviceName = service.getString("internalName");
        type(serviceSearchElement, serviceName);
    }

    public void checkService(int rowIndex, JSONObject service) {
        final String serviceName = service.getString("internalName");
        waitForElements(servicesNamesElements);
        checkText(servicesNamesElements.get(rowIndex), serviceName);
        final String visibility = isServiceVisible(service) ? "Visible" : "Hidden";
        checkText(servicesVisibilityElements.get(rowIndex), visibility);
        final String duration = (int) Math.ceil(service.getInt("duration") / 60.0) + " Minutes";
        checkText(servicesDurationElements.get(rowIndex), duration);
        checkText(servicesStatusElements.get(rowIndex), capitalize(service.getString("status")));
    }

    private void checkLinkUnlinkNotPresent() {
        waitForElement(servicesNameHeader);
        isElementInvisible(actionsHeader);
    }

    public void selectLocation(JSONObject location) {
        click(locationDropdown);
        waitForElement(locationDropdownContent);
        click(driver.findElement(By.cssSelector("[data-testid*='" + location.getString("id") + "']")));
    }

    public void checkPagination(List<JSONObject> services) {
        waitForElement(paginationContentElement);
        final int pagesCount = (int) Math.ceil(services.size() / 50.0);
        int elementNumber = 1;
        isElementDisabled(paginationPrevButtonElement);

        for (int i = 0; i < pagesCount; i++) {
            final int ofElements = Math.min((elementNumber + 50 - 1), services.size());
            String expectedText = elementNumber + " - " + ofElements + " of " + services.size();
            checkText(paginationContentElement, expectedText);
            final boolean isOnLastPage = i == pagesCount - 1;
            if (!isOnLastPage) {
                elementNumber = (i + 1) * 50 + 1;
                isElementEnabled(paginationNextElement);
                click(paginationNextElement);
            }
        }
        isElementDisabled(paginationNextElement);
    }

    public void checkSearchEmptyResult() {
        final String searchText = UUID.randomUUID().toString();
        type(serviceSearchElement, searchText);
        waitForElement(emptyResultElement);
        final String expectedText = "No results for \"" + searchText + "\"";
        checkText(emptyResultElement, expectedText);
    }

    public void checkNoServicesPage() {
        final String expectedText = "Sorry :(\n" + "There are no services";
        checkText(emptyResultElement, expectedText);
    }

    public void checkLocation(JSONObject anticipatedLocation) {
        checkText(locationDropdownContent, anticipatedLocation.getString("internalName"));
    }

    public void checkServiceActionToBe(int rowIndex, ToggleAction action) {
        waitForElements(servicesNamesElements);
        final String expectedText = action.equals(ToggleAction.UNLINK) ? "Unlink now" : "Link now";
        checkText(servicesLinkUnlinkElements.get(rowIndex), expectedText);
    }

    public void linkUnlinkLocationServiceToUser(int rowIndex, ToggleAction linkUnlink) {
        waitForElements(servicesNamesElements);
        if (linkUnlink.equals(ToggleAction.LINK)) {
            checkServiceActionToBe(rowIndex, ToggleAction.LINK);
            click(servicesLinkUnlinkElements.get(rowIndex));
            checkServiceActionToBe(rowIndex, ToggleAction.UNLINK);
        } else {
            checkServiceActionToBe(rowIndex, ToggleAction.UNLINK);
            click(servicesLinkUnlinkElements.get(rowIndex));
            checkServiceActionToBe(rowIndex, ToggleAction.LINK);
        }
    }

    public void checkNoLocation() {
        waitForElement(locationDropDownDisabled);
        checkText(locationDropDownDisabled, "No locations");
    }

    public void checkSingleLinkedLocation(JSONObject linkedLocation) {
        waitForElement(locationDropDownDisabled);
        checkText(locationDropDownDisabled, linkedLocation.getString("internalName"));
    }

    private boolean isServiceVisible(JSONObject service) {
        return service.getJSONObject("visibility").getBoolean("monitor")
                || service.getJSONObject("visibility").getBoolean("physicalKiosk")
                || service.getJSONObject("visibility").getBoolean("webKiosk");
    }

    public void searchForLocation(String locationName) {
        type(locationSearchInput, locationName);
    }

    public void checkLocationsInDropdown(List<JSONObject> locations) {
        waitForElements(locationsOptions);
        for (int i = 0; i < locations.size(); i++) {
            checkText(locationsOptions.get(i), locations.get(i).getString("internalName"));
        }
    }

    public void clearLocationDropdownSearchField() {
        clearText(locationSearchInput);
    }

    public void checkEmptySearchResultInLocationDropdown(String locationName) {
        checkText(locationDropdownEmptySearchElement, "No results for\n\"" + locationName + "\"");
    }

    public void checkServicesCount(List<JSONObject> services) {
        final String servicesCount = services.size()==0? "Services": "Services (" + services.size() + ")";
        checkText(servicesCountElement, servicesCount);
    }


    @Override
    public String getPageUrl() {
        return PAGE_URL + userToEnter.get().getString("id") + "/services";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(tableContainer);
    }

}
