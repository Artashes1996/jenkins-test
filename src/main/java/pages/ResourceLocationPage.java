package pages;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.commons.ToggleAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static utils.TestUtils.capitalize;

public class ResourceLocationPage extends BasePage<ResourceLocationPage> {

    private static final String PAGE_URL = UI_URI_CONSOLE + "/company/management/resource/";
    public String resourceId;

    @FindBy(css = "[data-testid='input-search']")
    private WebElement searchLocationsElement;
    @FindBy(css = "[data-testid='actions-header']")
    private WebElement locationLinkUnlinkColumnElement;
    @FindBy(css = "[data-testid='name']")
    private List<WebElement> locationNameElements;
    @FindBy(css = "[data-testid='location']")
    private List<WebElement> locationAddressElements;
    @FindBy(css = "[data-testid='zipcode']")
    private List<WebElement> locationZipcodeElements;
    @FindBy(css = "[data-testid='status']")
    private List<WebElement> locationStatusElements;
    @FindBy(css = "[data-testid='actions-link-unlink']")
    private List<WebElement> locationLinkUnlinkElements;
    @FindBy(css = "[data-testid=' table-loading']")
    private WebElement tableLoadingElement;
    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResultAfterSearchElement;
    @FindBy(css = "[data-testid='sort-INTERNAL_NAME']")
    private WebElement locationNameColumnElement;

    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement paginationPreviousButtonElement;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement paginationNextButtonElement;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationTextElement;

    public ResourceLocationPage(String browser, String version, String organizationId, String resourceId, String token) {
        super(browser, version, organizationId, token);
        this.resourceId = resourceId;
    }

    public void clickOnSortingIcon() {
        click(locationNameColumnElement);
    }

    public void searchForLocation(String locationAttribute) {
        type(searchLocationsElement, locationAttribute);
    }

    public void checkEmptySearchResult(String searchText) {
        checkText(emptyResultAfterSearchElement, "No results for \"" + searchText);
    }

    public void checkEmptyLocationPage() {
        checkText(emptyResultAfterSearchElement, "Sorry :(\n" +
                "There are no locations");
    }

    public enum Tabs {
        LOCATIONS(By.cssSelector("[data-testid='tab-item-locations']")),
        SERVICES(By.cssSelector("[data-testid='tab-item-services']"));

        private final By by;

        Tabs(By by) {
            this.by = by;
        }

        public By getBy() {
            return by;
        }
    }

    public void changeTab(ResourceLocationPage.Tabs tab) {
        click(driver.findElement(tab.getBy()));
    }

    public void checkLocationCountOfUserInPaginationText(int expectedCount) {
        isElementEnabled(paginationTextElement);
        final String[] pieces = paginationTextElement.getText().split(" ");
        assertEquals(pieces[pieces.length - 1], String.valueOf(expectedCount));
        isElementDisabled(paginationPreviousButtonElement);
        if (expectedCount > 50) {
            isElementEnabled(paginationNextButtonElement);
            click(paginationNextButtonElement);
            isElementEnabled(paginationPreviousButtonElement);
        } else {
            isElementDisabled(paginationPreviousButtonElement);
        }
    }

    public void checkIfLocationsAreSorted(List<JSONObject> locations) {
        waitForElementIsNotVisible(tableLoadingElement);
        waitForElements(locationNameElements);
        for (int i = 0; i < locations.size(); i++) {
            checkLocationDetailsByIndex(i, locations.get(i));
        }
    }

    public void checkLocationsInListByAdminRoles(List<JSONObject> linkedLocations, List<JSONObject> unlinkedLocations) {
        waitForElements(locationNameElements);
        for (int i = 0; i < linkedLocations.size(); i++) {
            checkLocationDetailsByIndex(i, linkedLocations.get(i));
            checkText(locationLinkUnlinkElements.get(i), "Unlink now");
        }
        for (int i = linkedLocations.size(); i < linkedLocations.size() + unlinkedLocations.size(); i++) {
            final int j = i - linkedLocations.size();
            checkLocationDetailsByIndex(i, unlinkedLocations.get(j));
            checkText(locationLinkUnlinkElements.get(i), "Link now");
        }
    }

    public void checkLocationsInListByStaff(List<JSONObject> locationsLinkedToStaff) {
        waitForElements(locationNameElements);
        for (int i = 0; i < locationsLinkedToStaff.size(); i++) {
            checkLocationDetailsByIndex(i, locationsLinkedToStaff.get(i));
        }
    }

    public void checkLocationsInListByLocationAdminRole(List<JSONObject> locationsLinkedToLocationAdmin, List<JSONObject> locationsLinkedToResource) {
        List<JSONObject> linkedToLocationAdminAndNotResource = locationsLinkedToLocationAdmin.stream()
                .filter(location -> !locationsLinkedToResource.contains(location)).collect(Collectors.toList());
        List<JSONObject> linkedToBoth = new ArrayList<>(locationsLinkedToResource);

        linkedToBoth.forEach(location -> {
            location.put("linked", locationsLinkedToLocationAdmin.contains(location));
        });
        linkedToLocationAdminAndNotResource = linkedToLocationAdminAndNotResource.stream()
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        linkedToBoth = linkedToBoth.stream()
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());

        waitForElements(locationNameElements);
        for (int i = 0; i < linkedToBoth.size(); i++) {
            checkLocationDetailsByIndex(i, linkedToBoth.get(i));
            checkText(locationLinkUnlinkElements.get(i), linkedToBoth.get(i).getBoolean("linked") ? "Unlink now" : "");
        }
        for (int i = linkedToBoth.size(); i < linkedToBoth.size() + linkedToLocationAdminAndNotResource.size(); i++) {
            final int j = i - linkedToBoth.size();
            checkLocationDetailsByIndex(i, linkedToLocationAdminAndNotResource.get(j));
            checkText(locationLinkUnlinkElements.get(i), "Link now");
        }
    }

    public void checkSearchResult(int index, JSONObject jsonObject) {
        waitForElementIsNotVisible(tableLoadingElement);
        checkLocationDetailsByIndex(index, jsonObject);
    }

    public void linkUnlinkLocationByIndex(int index, ToggleAction toggleAction) {
        waitForElements(locationLinkUnlinkElements);
        final String textToCheck = toggleAction.equals(ToggleAction.LINK) ? "Link now" : "Unlink now";
        checkText(locationLinkUnlinkElements.get(index), textToCheck);
        click(locationLinkUnlinkElements.get(index));
    }

    private void checkLocationDetailsByIndex(int index, JSONObject location) {
        checkText(locationNameElements.get(index), location.getString("internalName"));
        checkText(locationAddressElements.get(index), location.getJSONObject("address").getString("address"));
        checkText(locationZipcodeElements.get(index), location.getJSONObject("address").getString("zipcode"));
        checkText(locationStatusElements.get(index), capitalize(location.getString("status")));
    }

    public void checkLocationLinkUnlinkColumnMissing() {
        isElementInvisible(locationLinkUnlinkColumnElement);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + resourceId + "/locations/";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertTrue(driver.getCurrentUrl().contains(getPageUrl()));
        waitForElement(locationNameColumnElement);
    }
}
