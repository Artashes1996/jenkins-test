package pages;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static configuration.Config.UI_URI_CONSOLE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static utils.TestUtils.capitalize;
import static utils.TestUtils.verticalFormatter;

public class UserLocationPage extends BasePage<UserLocationPage> {

    private static final String PAGE_URL = UI_URI_CONSOLE + "/company/user/";

    @FindBy(css = "[class^='_user-full-name_']")
    private WebElement userFullNameElement;
    @FindBy(css = "[data-testid='user-role']")
    private WebElement userRoleElement;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement paginationPreviousButton;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement paginationNextButton;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationText;
    @FindBy(css = "[placeholder='Search locations']")
    private WebElement searchLocationInput;
    @FindBy(css = "[data-testid='sort-INTERNAL_NAME']")
    private WebElement locationNameColumn;
    @FindBy(css = "[data-testid='actions-header']")
    private WebElement locationLinkUnlinkColumn;

    @FindBy(css = "[data-testid='name']")
    private List<WebElement> locationNames;
    @FindBy(css = "[data-testid='location']")
    private List<WebElement> locationAddresses;
    @FindBy(css = "[data-testid='zipcode']")
    private List<WebElement> locationZipcodes;
    @FindBy(css = "[data-testid='status']")
    private List<WebElement> locationStatuses;
    @FindBy(css = "[data-testid='type']")
    private List<WebElement> locationTypes;
    @FindBy(css = "[data-testid='actions-link-unlink']")
    private List<WebElement> locationLinkUnlinkAction;

    @FindBy(css = "[data-testid='empty-text']")
    private WebElement emptyLocationDropDownText;
    @FindBy(css = "[data-testid='empty-result']")
    private WebElement emptyResultAfterSearch;
    @FindBy(css = "[data-testid='danger-toast']")
    private WebElement errorToast;


    public UserLocationPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    public UserLocationPage(String browser, String version, String token) {
        super(browser, version, token);
    }

    public UserLocationPage(String browser, String version) {
        super(browser, version);
    }

    public enum Tabs {
        PERSONAL_DETAILS(By.xpath("//*[contains(@class, '_tabs_1eiio_1')]/button[contains(text(), 'Personal')]")),
        LOCATIONS(By.xpath("//*[contains(@class, '_tabs_1eiio_1')]/button[contains(text(), 'locations')]")),
        SERVICES(By.xpath("//*[contains(@class, '_tabs_1eiio_1')]/button[contains(text(), 'services')]"));

        private By by;

        Tabs(By by) {
            this.by = by;
        }

        public By getBy() {
            return by;
        }
    }

    public void checkUserInfo(JSONObject user) {
        checkText(userFullNameElement, user.getString("firstName") + " " + user.getString("lastName"));
        checkText(userRoleElement, capitalize(verticalFormatter(user.getString("role"))));
    }

    public void checkLocationCountOfUserInPaginationText(int expectedCount) {
        isElementEnabled(paginationText);
        final String[] pieces = paginationText.getText().split(" ");
        assertEquals(pieces[pieces.length - 1], String.valueOf(expectedCount));
        isElementDisabled(paginationPreviousButton);
        if (expectedCount > 50) {
            isElementEnabled(paginationNextButton);
            click(paginationNextButton);
            isElementEnabled(paginationPreviousButton);
        } else {
            isElementDisabled(paginationPreviousButton);
        }
    }

    public void searchForLocation(String locationAttribute) {
        type(searchLocationInput, locationAttribute);
    }

    public void checkLocationsInListByAdminRoles(List<JSONObject> linkedLocations, List<JSONObject> unlinkedLocations) {
        waitForElements(locationNames);
        for (int i = 0; i < linkedLocations.size(); i++) {
            checkText(locationNames.get(i), linkedLocations.get(i).getString("internalName"));
            if (linkedLocations.get(i).getString("type").equals("VIRTUAL")) {
                checkText(locationAddresses.get(i), "N/A");
                checkText(locationZipcodes.get(i), "N/A");
            } else {
                checkText(locationAddresses.get(i), linkedLocations.get(i).getJSONObject("address").getString("address"));
                checkText(locationZipcodes.get(i), linkedLocations.get(i).getJSONObject("address").getString("zipcode"));
            }

            checkText(locationStatuses.get(i), capitalize(linkedLocations.get(i).getString("status")));
            checkText(locationTypes.get(i), capitalize(linkedLocations.get(i).getString("type")));
            checkText(locationLinkUnlinkAction.get(i), "Unlink");
        }

        for (int i = linkedLocations.size(); i < linkedLocations.size() + unlinkedLocations.size(); i++) {
            final int j = i - linkedLocations.size();
            checkText(locationNames.get(i), unlinkedLocations.get(j).getString("internalName"));
            if (unlinkedLocations.get(j).getString("type").equals("VIRTUAL")) {
                checkText(locationAddresses.get(i), "N/A");
                checkText(locationZipcodes.get(i), "N/A");
            } else {
                checkText(locationAddresses.get(i), unlinkedLocations.get(j).getJSONObject("address").getString("address"));
                checkText(locationZipcodes.get(i), unlinkedLocations.get(j).getJSONObject("address").getString("zipcode"));
            }
            checkText(locationStatuses.get(i), capitalize(unlinkedLocations.get(j).getString("status")));
            checkText(locationTypes.get(i), capitalize(unlinkedLocations.get(j).getString("type")));
            checkText(locationLinkUnlinkAction.get(i), "Link now");
        }
    }

    public void checkLocationsInListByLocationAdminRole(List<JSONObject> locationsLinkedToLocationAdmin, List<JSONObject> locationsLinkedToUser) {
        List<JSONObject> linkedToLocationAdminAndNotUser = locationsLinkedToLocationAdmin.stream()
                .filter(location -> !locationsLinkedToUser.contains(location)).collect(Collectors.toList());
        List<JSONObject> linkedToBoth = new ArrayList<>(locationsLinkedToUser);

        linkedToBoth.forEach(location -> {
            location.put("linked", locationsLinkedToLocationAdmin.contains(location));
        });
        linkedToLocationAdminAndNotUser = linkedToLocationAdminAndNotUser.stream()
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        linkedToBoth = linkedToBoth.stream()
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());

        waitForElements(locationNames);
        for (int i = 0; i < linkedToBoth.size(); i++) {
            checkText(locationNames.get(i), linkedToBoth.get(i).getString("internalName"));
            if (linkedToBoth.get(i).getString("type").equals("VIRTUAL")) {
                checkText(locationAddresses.get(i), "N/A");
                checkText(locationZipcodes.get(i), "N/A");
            } else {
                checkText(locationAddresses.get(i), linkedToBoth.get(i).getJSONObject("address").getString("address"));
                checkText(locationZipcodes.get(i), linkedToBoth.get(i).getJSONObject("address").getString("zipcode"));
            }
            checkText(locationStatuses.get(i), capitalize(linkedToBoth.get(i).getString("status")));
            checkText(locationTypes.get(i), capitalize(linkedToBoth.get(i).getString("type")));
            checkText(locationLinkUnlinkAction.get(i), linkedToBoth.get(i).getBoolean("linked") ? "Unlink" : "");
        }
        for (int i = linkedToBoth.size(); i < linkedToBoth.size() + linkedToLocationAdminAndNotUser.size(); i++) {
            final int j = i - linkedToBoth.size();
            checkText(locationNames.get(i), linkedToLocationAdminAndNotUser.get(j).getString("internalName"));
            if (linkedToLocationAdminAndNotUser.get(j).getString("type").equals("VIRTUAL")) {
                checkText(locationAddresses.get(i), "N/A");
                checkText(locationZipcodes.get(i), "N/A");
            } else {
                checkText(locationAddresses.get(i), linkedToLocationAdminAndNotUser.get(j).getJSONObject("address").getString("address"));
                checkText(locationZipcodes.get(i), linkedToLocationAdminAndNotUser.get(j).getJSONObject("address").getString("zipcode"));
            }
            checkText(locationStatuses.get(i), capitalize(linkedToLocationAdminAndNotUser.get(j).getString("status")));
            checkText(locationTypes.get(i), capitalize(linkedToLocationAdminAndNotUser.get(j).getString("type")));
            checkText(locationLinkUnlinkAction.get(i), "Link now");
        }
    }

    public void checkLocationsInListByStaff(List<JSONObject> linkedToUser) {
        waitForElements(locationNames);
        for (int i = 0; i < linkedToUser.size(); i++) {
            checkText(locationNames.get(i), linkedToUser.get(i).getString("internalName"));
            if (linkedToUser.get(i).getString("type").equals("VIRTUAL")) {
                checkText(locationAddresses.get(i), "N/A");
                checkText(locationZipcodes.get(i), "N/A");
            } else {
                checkText(locationAddresses.get(i), linkedToUser.get(i).getJSONObject("address").getString("address"));
                checkText(locationZipcodes.get(i), linkedToUser.get(i).getJSONObject("address").getString("zipcode"));
            }

            checkText(locationStatuses.get(i), capitalize(linkedToUser.get(i).getString("status")));
            checkText(locationTypes.get(i), capitalize(linkedToUser.get(i).getString("type")));
        }
    }

    public void checkLocationLinkUnlinkColumnMissing() {
        isElementInvisible(locationLinkUnlinkColumn);
    }

    public void checkEmptyLocationPage() {
        checkText(emptyLocationDropDownText, "No locations found");
    }

    public void checkEmptySearchResult(String searchText) {
        checkText(emptyResultAfterSearch, "No results for \"" + searchText);
    }

    public void orderByName() {
        click(locationNameColumn);
    }

    public void checkLocationsInPage(List<JSONObject> locations) {
        waitForElements(locationNames);
        assertEquals(locationNames.size(), locations.size());
        for (int i = 0; i < locations.size(); i++) {
            checkText(locationNames.get(i), locations.get(i).getString("internalName"));
        }
    }

    public void linkUnlinkLocationByIndex(int index) {
        waitForElements(locationLinkUnlinkAction);
        final String text = locationLinkUnlinkAction.get(index).getText();
        click(locationLinkUnlinkAction.get(index));
        checkText(locationLinkUnlinkAction.get(index), text.equals("Unlink") ? "Link now" : "Unlink");
    }

    public void checkLinkedLocationDetailsByIndex(int index, JSONObject location) {
        checkText(locationNames.get(index), location.getString("internalName"));
        if (location.getString("type").equals("VIRTUAL")) {
            checkText(locationAddresses.get(index), "N/A");
            checkText(locationZipcodes.get(index), "N/A");
        } else {
            checkText(locationAddresses.get(index), location.getJSONObject("address").getString("address"));
            checkText(locationZipcodes.get(index), location.getJSONObject("address").getString("zipcode"));
        }
        checkText(locationStatuses.get(index), capitalize(location.getString("status")));
        checkText(locationTypes.get(index), capitalize(location.getString("type")));
        checkText(locationLinkUnlinkAction.get(index), "Unlink");
    }

    public void checkUnlinkedLocationDetailsByIndex(int index, JSONObject location) {
        waitForElements(locationNames);
        checkText(locationNames.get(index), location.getString("internalName"));
        if (location.getString("type").equals("VIRTUAL")) {
            checkText(locationAddresses.get(index), "N/A");
            checkText(locationZipcodes.get(index), "N/A");
        } else {
            checkText(locationAddresses.get(index), location.getJSONObject("address").getString("address"));
            checkText(locationZipcodes.get(index), location.getJSONObject("address").getString("zipcode"));
        }
        checkText(locationStatuses.get(index), capitalize(location.getString("status")));
        checkText(locationTypes.get(index), capitalize(location.getString("type")));
        checkText(locationLinkUnlinkAction.get(index), "Link now");
    }

    public void changeTab(Tabs tab) {
        click(driver.findElement(tab.getBy()));
    }

    public void lastLocationUnlinkFromStaffOrLocationAdminErrorMessage(){
        final String text = "At least one location is required. Please first link another location to the user.";
        checkText(errorToast, text);
    }

    @Override
    public String getPageUrl() {
        return PAGE_URL + userToEnter.get().getString("id") + "/locations";
    }

    @Override
    protected void load() {
        driver.get(getPageUrl());
    }

    @Override
    protected void isLoaded() throws Error {
        assertEquals(driver.getCurrentUrl(), getPageUrl());
        waitForElement(locationNameColumn);
    }
}
