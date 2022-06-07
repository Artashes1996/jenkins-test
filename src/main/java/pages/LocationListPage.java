package pages;

import helpers.BaseUIHelper;
import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import utils.TestUtils;

import java.util.List;
import java.util.stream.IntStream;

import static configuration.Config.UI_URI_CONSOLE;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.LocationStatuses.*;
import static org.testng.Assert.*;

public class LocationListPage extends BasePage<LocationListPage> {

    private final String PAGE_URL = UI_URI_CONSOLE + "/company/locations";

    @FindBy(css = "[data-test='select']")
    private List<WebElement> checkboxes;
    @FindBy(css = "[data-testid='name']")
    private List<WebElement> locationNames;
    @FindBy(css = "[data-testid='type']")
    private List<WebElement> locationTypes;
    @FindBy(css = "[data-testid='location']")
    private List<WebElement> locationAddresses;
    @FindBy(css = "[data-testid='status']")
    private List<WebElement> locationStatuses;
    @FindBy(css = "[data-testid='zipcode']")
    private List<WebElement> zipcodes;
    @FindBy(css = "[data-testid='paginationContent']")
    private WebElement paginationText;
    @FindBy(css = "[data-testid='paginationPrevButton']")
    private WebElement previousButton;
    @FindBy(css = "[data-testid='paginationNextButton']")
    private WebElement nextButton;
    @FindBy(css = "[data-testid='success-toast']")
    private WebElement successToast;
    @FindBy(css = "[data-testid='input-search']")
    private WebElement searchLocationElement;

    @FindBy(xpath = "//*[@data-test='select-all']/..")
    private WebElement selectAll;

    @FindBy(xpath = "//*[@data-test='select']/.." )
    private WebElement select;

    public LocationListPage(String browser, String version) {
        super(browser, version);
    }

    public LocationListPage(String browser, String version, String organizationId, String token) {
        super(browser, version, organizationId, token);
    }

    //TODO add checks after location full detail implementation
    public void checkLocationInList(int index, JSONObject location) {
        waitForElements(locationNames);
        checkText(locationNames.get(index), location.getString(INTERNAL_NAME));
        final String statusToCheck = location.getString(STATUS).equals(ACTIVE.name())? TestUtils.capitalize(ACTIVE.name()):TestUtils.capitalize(INACTIVE.name());
        checkText(locationStatuses.get(index), statusToCheck);
//        checkText(zipcodes.get(index), location.getString(ZIPCODE));

    }

    public void checkAllLocationsList(List<JSONObject> expectedLocations) {
        waitForElements(locationNames);
        IntStream.range(0, expectedLocations.size()).forEach(index -> checkLocationInList(index, expectedLocations.get(index)));
    }

    // TODO how to change this?
    public void checkLocationCountOfPage(int givenLocationNumber){
        if (checkboxes.size() == locationNames.size())
            assertEquals(givenLocationNumber, checkboxes.size());
        else
            assertEquals(checkboxes.size(), locationNames.size());
    }

    public void checkLocationCreationSuccessToastMessage() {
        final String successMsg = "SUCCESS\nThe location has been created successfully.!";
        checkText(successToast, successMsg);
    }

    public void clickOnNextButton(){ click(nextButton);
    }

    public void clickOnPreviousButton(){
        click(previousButton);
    }

    public void checkPaginationText(String expectedText){

        checkText(paginationText, expectedText);
    }

    public void checkNextButtonEnabled(){

        isElementEnabled(nextButton);
    }

    public void checkPreviousButtonEnabled(){

        isElementEnabled(previousButton);
    }

    public void checkNextButtonDisabled(){

        isElementDisabled(nextButton);
    }

    public void checkPreviousButtonDisabled(){

        isElementDisabled(previousButton);
    }


    @Override
    public String getPageUrl() {
        return PAGE_URL;
    }

    @Override
    protected void load() {
        BaseUIHelper.getDriver(browser, version).get( PAGE_URL);
    }

    @Override
    public void isLoaded() throws Error {
        assertTrue(BaseUIHelper.getDriver(browser, version).getCurrentUrl().contains(PAGE_URL));
        waitForElement(searchLocationElement);
    }

}
