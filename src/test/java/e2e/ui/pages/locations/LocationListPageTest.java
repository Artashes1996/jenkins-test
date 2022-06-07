package e2e.ui.pages.locations;

import e2e.ui.pages.BasePageTest;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;
import pages.CreateLocationPage;
import pages.LocationListPage;
import pages.SignInPage;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;

import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.INTERNAL_NAME;

public class LocationListPageTest extends BasePageTest {

    private static final Logger LOGGER  = Logger.getLogger(LocationListPageTest.class);

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-1900", test = "PEG-2450")
    @Test
    public void checkEmptyState() {
        final String newOrganizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final LocationListPage locationListPage = new LocationListPage(browserToUse, versionToBe, newOrganizationId, supportToken);
        locationListPage.openPage();
        locationListPage.checkLocationCountOfPage(0);
    }

    // TODO split into multiple tests
    @Xray(requirement = "PEG-1900", test = "PEG-2451")
    @Test
    public void addNewLocationAndCheckOrder() {
        LOGGER.trace("<-------- PRECONDITION of PEG-2451 ----------");
        final JSONObject ownerAndOrganization = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final String organizationId = ownerAndOrganization.getJSONObject("ORGANIZATION").getString("id");

        new LocationFlows().createLocations(organizationId,50);
        final String locationName = "111 " + new Random().nextInt();
        final JSONObject location = new CreateLocationRequestBody().bodyBuilder(CreateLocationRequestBody.CreateLocationCombination.ALL_FIELDS);
        location.put(INTERNAL_NAME, locationName);

        LOGGER.trace("<--------- STEPS & VALIDATIONS of PEG-2451 -----------");
        LocationListPage locationListPage = new LocationListPage(browserToUse, versionToBe, organizationId, supportToken);
        locationListPage.openPage();
        locationListPage.checkPaginationText("1 - 50 of 50");
        locationListPage.checkNextButtonDisabled();
        locationListPage.checkPreviousButtonDisabled();

        CreateLocationPage createLocationPage = new CreateLocationPage(browserToUse, versionToBe,organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.fillLocationGeneralInfo(location);

        locationListPage.checkNextButtonEnabled();
        locationListPage.checkPreviousButtonDisabled();
        locationListPage.checkPaginationText("1 - 50 of 51");
        locationListPage.checkLocationInList(0, location);

        locationListPage.clickOnNextButton();
        locationListPage.checkPaginationText("51 - 51 of 51");
        locationListPage.checkNextButtonDisabled();
        locationListPage.checkPreviousButtonEnabled();

        createLocationPage = new CreateLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        createLocationPage.openPage();
        createLocationPage.clickCancel();
        locationListPage = new LocationListPage(browserToUse, versionToBe, organizationId,supportToken);
        locationListPage.openPage();
        locationListPage.checkLocationInList(0, location);
        locationListPage.checkPaginationText("1 - 50 of 51");
    }

    @Xray(requirement = "PEG-1900", test = "PEG-2452")
    @Test
    public void checkLocationOrder() {
        final JSONObject ownerAndOrganization = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        final String organizationId = ownerAndOrganization.getJSONObject("ORGANIZATION").getString("id");
        final List<JSONObject> locations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            locations.add(new LocationFlows().createLocation(organizationId));
        }
        final LocationListPage locationListPage = new LocationListPage(browserToUse, versionToBe, organizationId, supportToken);
        locationListPage.openPage();
        locations.stream()
                .sorted(Comparator.comparing((JSONObject location) -> location.getString(INTERNAL_NAME)))
                .collect(Collectors.toList());

        locationListPage.checkAllLocationsList(locations);
    }

}
