package e2e.ui.pages.locations;

import e2e.ui.pages.BasePageTest;
import helpers.appsapi.locationsresource.payloads.Countries;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.*;
import utils.TestUtils;
import utils.Xray;

import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;

public class CreateLocationPageTest extends BasePageTest {

    private String organizationId;
    private LocationFlows locationFlows;
    private CreateLocationRequestBody createLocationRequestBody;


    @BeforeClass
    public void setup() {
        locationFlows = new LocationFlows();
        createLocationRequestBody = new CreateLocationRequestBody();
        final JSONObject ownerAndOrganization = new OrganizationFlows().createPausedOrganizationWithOwner();
        organizationId = ownerAndOrganization.getJSONObject("ORGANIZATION").getString("id");
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-1696", test = "PEG-2121")
    @Test
    public void checkInvalidPhoneNumberError() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.fillPhoneNumber("+" + TestUtils.getRandomInt(8));
        createLocationPage.clickCreate();
        createLocationPage.checkInvalidPhoneNumberErrorMsg();
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7020")
    @Test
    public void checkRequiredFields() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.clickCreate();
        createLocationPage.checkRequiredFieldErrorMessages();
    }

    @Xray(requirement = "PEG-1696", test = "PEG-2449")
    @Test
    public void createLocationWithExistingName() {
        final String locationName = locationFlows.createLocation(organizationId).getString("internalName");
        final JSONObject location = createLocationRequestBody.bodyBuilder(CreateLocationCombination.REQUIRED_FIELDS);
        location.put(INTERNAL_NAME, locationName);
        location.getJSONObject("address").put(COUNTRY, Countries.BELGIUM.getCountry());
        location.put(TIMEZONE, Countries.BELGIUM.getTimeZone());
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.fillLocationGeneralInfo(location);
        createLocationPage.fillLocationAddressData(location);
        createLocationPage.clickCreate();
        createLocationPage.checkExistingLocationErrorMsg();
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7021")
    @Test
    public void stateRegionFieldNotDisplayed() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.selectCountry("Germany");
        createLocationPage.checkIfStateDropdownNotVisible();
        createLocationPage.selectCountry("Belgium");
        createLocationPage.checkIfStateDropdownNotVisible();
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7024")
    @Test
    public void checkCancelButtonFunctionality() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.clickCancel();
        final LocationListPage locationListPage = new LocationListPage(browserToUse, versionToBe);
        locationListPage.isLoaded();
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7020")
    @Test
    public void createLocation() {
        final Countries country = Countries.getRandomCountry();
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        final JSONObject location = createLocationRequestBody.bodyBuilder(CreateLocationRequestBody.CreateLocationCombination.ALL_FIELDS);
        location.getJSONObject("address").put(COUNTRY, country.getCountry());
        if (country.getState() == null) {
            location.getJSONObject("address").remove(STATE_REGION);
        } else {
            location.getJSONObject("address").put(STATE_REGION, country.getState());
        }
        location.put(TIMEZONE, country.getTimeZone());
        createLocationPage.fillLocationGeneralInfo(location);
        createLocationPage.fillLocationAddressData(location);
        createLocationPage.clickCreate();
        createLocationPage.checkSuccessMsg();
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7026")
    @Test
    public void createLocationWithAutosuggestion() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        final JSONObject location = createLocationRequestBody.bodyBuilder(CreateLocationRequestBody.CreateLocationCombination.ALL_FIELDS);
        createLocationPage.fillLocationGeneralInfo(location);
        createLocationPage.fillAddressLine1(String.valueOf(TestUtils.getRandomInt(3)), true);
        createLocationPage.clickCreate();
        createLocationPage.checkSuccessMsg();
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7028")
    @Test
    public void notChangeAlreadyFilledValues() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.fillAddressLine1(String.valueOf(TestUtils.getRandomInt(3)), true);
        final JSONObject addressInfo = createLocationPage.getAddressInfo();
        createLocationPage.fillAddressLine1("address_line_1", false);
        createLocationPage.checkAddressDetails(addressInfo);
    }

    @Xray(requirement = "PEG-6658", test = "PEG-7031")
    @Test
    public void resetValuesAfterChangingCountry() {
        final CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        final JSONObject location = createLocationRequestBody.bodyBuilder(CreateLocationRequestBody.CreateLocationCombination.ALL_FIELDS);
        location.getJSONObject("address").put(COUNTRY, "Canada");
        location.getJSONObject("address").put(STATE_REGION, "Nunavut");
        location.put(TIMEZONE, "America/Dawson_Creek (UTC-07:00)");
        createLocationPage.fillLocationGeneralInfo(location);
        createLocationPage.selectCountry("Australia");
        createLocationPage.checkEmptyFields();
    }
}

