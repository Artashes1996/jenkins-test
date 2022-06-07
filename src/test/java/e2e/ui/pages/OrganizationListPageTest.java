package e2e.ui.pages;

import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.CreateOrganization;
import pages.HomePage;
import pages.OrganizationListPage;
import pages.SignInPage;
import utils.RetryAnalyzer;
import utils.Xray;

import java.util.*;

import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.*;
import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.OrganizationSortingKey.*;
import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.SortDirection.*;
import static pages.OrganizationListPage.Actions.*;
import static utils.TestUtils.FAKER;

public class OrganizationListPageTest extends BasePageTest {

    private String blockedOrganizationName;
    private String liveOrganizationName;
    private static final OrganizationFlows ORGANIZATION_FLOWS = new OrganizationFlows();
    private static final UserFlows USER_FLOWS = new UserFlows();

    @BeforeClass
    void browserSetup() {

        blockedOrganizationName = ORGANIZATION_FLOWS.createBlockedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        liveOrganizationName = ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final int organizationCountInOnePage = 50;
        final int totalOrganizationCount = OrganizationsHelper.searchOrganizations(supportToken, new JSONObject())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("totalElements");
        if (totalOrganizationCount < organizationCountInOnePage) {
            for (int i = 0; i < organizationCountInOnePage - totalOrganizationCount + 1; i++) {
                ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner();
            }
        }
    }

    @BeforeMethod
    final void init() { new SignInPage(browserToUse, versionToBe).openPage(); }

    @Xray(requirement = "PEG-2125", test = "PEG-2447")
    @Test
    public void checkPagination() {
        final int totalElements = OrganizationsHelper.searchOrganizations(supportToken, new JSONObject())
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().path("totalElements");

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        organizationListPage.checkOrganizationCount(totalElements);
    }

    @Xray(requirement = "PEG-2131", test = "PEG-2448")
    @Test
    public void createOrganizationAndCheckOnList() {

        final String newOrgName = FAKER.artist().name() + UUID.randomUUID();
        final CreateOrganization createOrganization = new CreateOrganization(browserToUse, versionToBe, supportToken);
        createOrganization.openPage();
        final HomePage homePage = createOrganization.createOrganization(null, newOrgName , "https://www.sfl.am", "+37455673856");
        homePage.checkSuccessToastMessage();

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        organizationListPage.searchForOrganization(newOrgName);
        organizationListPage.checkOrganizationInList(0, newOrgName, "Paused", "Education");
    }

    // TODO split into multiple tests
    @Xray(requirement = "PEG-2156", test = "PEG-2769")
    @Test
    public void checkSortOfOrganizationsByNameAndUserCount() {
        final JSONObject jsonObject = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(SIZE, 50);
        pagination.put(SORT, INTERNAL_NAME + ":" + ASC);
        jsonObject.put(PAGINATION, pagination);
        final ArrayList<Object> firstPageOrganizationNameAsc = OrganizationsHelper.searchOrganizations(supportToken, jsonObject)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content");

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        for (int i = 0; i< firstPageOrganizationNameAsc.size()-40; i++) {
            final Map<String,String> organization = (Map<String, String>) firstPageOrganizationNameAsc.get(i);
            organizationListPage.checkOrganizationInList(i, organization.get("internalName"), organization.get("status"), organization.get("vertical"));
        }
        organizationListPage.sortByUserCount();
        pagination.put(SORT, NUMBER_OF_USERS + ":" + ASC + "," + INTERNAL_NAME + ":" + ASC);
        final ArrayList<JSONObject> firstPageUserCountAsc = OrganizationsHelper.searchOrganizations(supportToken, jsonObject)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().path("content");
        for (int i = 0; i< firstPageUserCountAsc.size()-40; i++) {
            final Map<String,String> organization = (Map<String, String>) firstPageUserCountAsc.get(i);
            organizationListPage.checkOrganizationInList(i, organization.get("internalName"), organization.get("status"), organization.get("vertical"));
        }
        organizationListPage.clickOnNextButton();
        pagination.put(PAGE, 1);
        final ArrayList<JSONObject> secondPageUserCountAsc = OrganizationsHelper.searchOrganizations(supportToken, jsonObject)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract().path("content");
        for (int i = 0; i< secondPageUserCountAsc.size()-40; i++) {
            final Map<String,String> organization = (Map<String, String>) secondPageUserCountAsc.get(i);
            organizationListPage.checkOrganizationInList(i, organization.get("internalName"), organization.get("status"), organization.get("vertical"));
        }
    }

    @Xray(requirement = "PEG-2156", test = "PEG-2770")
    @Test
    public void checkSortOfOrganizationsByLocationCount(){
        final JSONObject jsonObject = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(SIZE, 50);
        pagination.put(SORT, NUMBER_OF_LOCATIONS + ":" + ASC + "," + INTERNAL_NAME + ":" + ASC);
        jsonObject.put(PAGINATION, pagination);

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        organizationListPage.clickOnNextButton();
        organizationListPage.sortByLocationCount();

        final ArrayList<Object> firstPageOrganizationLocationCountAsc = OrganizationsHelper.searchOrganizations(supportToken, jsonObject)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content");
        for (int i = 0; i< firstPageOrganizationLocationCountAsc.size()-40; i++) {
            final Map<String,String> organization = (Map<String, String>) firstPageOrganizationLocationCountAsc.get(i);
            organizationListPage.checkOrganizationInList(i, organization.get("internalName"), organization.get("status"), organization.get("vertical"));
        }
        organizationListPage.checkNextButtonEnabled();
        organizationListPage.checkPreviousButtonDisabled();
    }

    @Xray(requirement = "PEG-2035", test = "PEG-2771")
    @Test
    public void searchForOrganization(){
        final String randomString = String.valueOf(10 + new Random().nextInt(90));
        final JSONObject jsonObject = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(SIZE, 50);
        pagination.put(SORT,INTERNAL_NAME + ":" + ASC);
        jsonObject.put(PAGINATION, pagination);
        jsonObject.put(QUERY, randomString);

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        organizationListPage.searchForOrganization(randomString);

        final ArrayList<Object> organizationSearchResult = OrganizationsHelper.searchOrganizations(supportToken, jsonObject)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content");
        for (int i = 0; i< organizationSearchResult.size(); i++) {
            final Map<String,String> organization = (Map<String, String>) organizationSearchResult.get(i);
            organizationListPage.checkOrganizationInList(i, organization.get("internalName"), organization.get("status"), organization.get("vertical"));
        }
    }

    @Xray(requirement = "PEG-2156", test = "PEG-2772")
    @Test
    public void searchAndOrderWithDescOrder(){
        final String randomString = String.valueOf(10 + new Random().nextInt(90));
        final JSONObject jsonObject = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(SIZE, 50);
        pagination.put(SORT,NUMBER_OF_USERS + ":" + DESC + "," + INTERNAL_NAME + ":" + ASC);
        jsonObject.put(PAGINATION, pagination);
        jsonObject.put(QUERY, randomString);

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        organizationListPage.sortByUserCount();
        organizationListPage.sortByUserCount();
        organizationListPage.searchForOrganization(randomString);

        final ArrayList<Object> organizationSearchResult = OrganizationsHelper.searchOrganizations(supportToken, jsonObject)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content");
        for (int i = 0; i< organizationSearchResult.size(); i++) {
            final Map<String,String> organization = (Map<String, String>) organizationSearchResult.get(i);
            organizationListPage.checkOrganizationInList(i, organization.get("internalName"), organization.get("status"), organization.get("vertical"));
        }
    }

    @Xray(requirement = "PEG-2345", test = "PEG-3633")
    @Test
    public void checkOrganizationPublishWithoutActiveOwner() {
        final JSONObject organizationObject = ORGANIZATION_FLOWS.createUnpublishedOrganization();
        final String organizationName = organizationObject.getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.publishOrganization();
        newOrganizationListPage.confirmPublishOperation(organizationName);
        newOrganizationListPage.checkErrorToastForAnActiveOwnerAccount();
    }

    @Xray(requirement = "PEG-2345", test = "PEG-3634")
    @Test
    public void checkOrganizationPublishWithoutPOC() {
        final JSONObject organizationObject = ORGANIZATION_FLOWS.createUnpublishedOrganization();
        final String organizationId = organizationObject.getString("id");
        final String organizationName = organizationObject.getString("internalName");
        USER_FLOWS.createOwnerWithOrganizationIdWithoutPOC(organizationId);
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.publishOrganization();
        newOrganizationListPage.confirmPublishOperation(organizationName);
        newOrganizationListPage.checkErrorToastForAnActivePOC();
    }

    @Xray(requirement = "PEG-2345", test = "PEG-3638")
    @Test
    public void checkCancelActionFromPublishOrganizationPopup() {
        final JSONObject organizationObject = ORGANIZATION_FLOWS.createUnpublishedOrganization();
        final String organizationId = organizationObject.getString("id");
        final String organizationName = organizationObject.getString("internalName");
        USER_FLOWS.createOwnerWithOrganizationIdWithoutPOC(organizationId);
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.cancelPublishingOrganization();
        newOrganizationListPage.cancelPublishOperation(organizationName);
        newOrganizationListPage.checkNoPopupIsDisplayed();
   }

    @Xray(requirement = "PEG-2345", test = "PEG-3639")
    @Test
    public void checkPublishOrganization() {

        final String organizationName = ORGANIZATION_FLOWS.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.publishOrganization();
        newOrganizationListPage.confirmPublishOperation(organizationName);
        newOrganizationListPage.checkOrganizationStatus("Live");
    }

    @Xray(requirement = "PEG-2201", test = "PEG-3834")
    @Test
    public void checkPauseOrganization() {

        final String organizationName = ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.pauseOrganization();
        newOrganizationListPage.confirmPauseOperation(organizationName);
        newOrganizationListPage.checkSuccessToastForAnPauseOrganization();
        newOrganizationListPage.checkOrganizationStatus("Paused");
    }

    @Xray(requirement = "PEG-2201", test = "PEG-3836")
    @Test
    public void checkUnPauseOrganization() {

        final String organizationName = ORGANIZATION_FLOWS.createPausedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.unPauseOrganization();
        newOrganizationListPage.checkSuccessToastForAnUnPauseOrganization();
        newOrganizationListPage.checkOrganizationStatus("Live");
    }

    @Xray(requirement = "PEG-2201", test = "PEG-3832")
    @Test
    public void checkCancelActionFromPauseOrganizationPopup() {

        final String organizationName = ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.cancelPausedOrganization();
        newOrganizationListPage.confirmPauseOperation(organizationName);
        newOrganizationListPage.checkNoPopupIsDisplayed();
    }

    @Xray(requirement = "PEG-2201", test = "PEG-3863")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkCloseIconFunctionalityFromPauseOrganizationPopup() {

        final String organizationName = ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.pauseOrganization();
        newOrganizationListPage.clickOnCloseIcon();
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4602")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void blockPausedOrganizationWithReasonOther() {

        final String organizationName = ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(BLOCK);
        newOrganizationListPage.clickOnOtherReason();
        newOrganizationListPage.typeReason(FAKER.harryPotter().quote());
        newOrganizationListPage.clickConfirmFromPopup();
        newOrganizationListPage.checkSuccessToastForBlockOrganization();
        newOrganizationListPage.checkOrganizationStatus(organizationName,"Blocked");

    }

    @Xray(requirement = "PEG-2046", test = "PEG-4596")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void blockLiveOrganizationWithReasonLackOfPayment() {

        final String organizationName = ORGANIZATION_FLOWS.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(BLOCK);
        newOrganizationListPage.checkThatFirstRadioButtonIsSelected();
        newOrganizationListPage.clickConfirmFromPopup();
        newOrganizationListPage.checkSuccessToastForBlockOrganization();
        newOrganizationListPage.checkOrganizationStatus(organizationName,"Blocked");
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4603")
    @Test
    public void unblockLiveWithDefaultReason() {

        final String organizationName = ORGANIZATION_FLOWS.createBlockedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(UNBLOCK);
        newOrganizationListPage.checkUnblockingPopupContent(organizationName);
        newOrganizationListPage.checkThatFirstRadioButtonIsSelected();
        newOrganizationListPage.clickConfirmFromPopup();
        newOrganizationListPage.checkSuccessToastForUnblockOrganization();
        newOrganizationListPage.checkOrganizationStatus(organizationName,"Live");
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4604")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void unblockPausedWithPaymentReceived() {

        final JSONObject organizationObject = ORGANIZATION_FLOWS.createPausedOrganizationWithOwner().getJSONObject("ORGANIZATION");
        ORGANIZATION_FLOWS.blockOrganization(organizationObject.getString("id"));
        final String organizationName = organizationObject.getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(UNBLOCK);
        newOrganizationListPage.checkUnblockingPopupContent(organizationName);
        newOrganizationListPage.clickOnPaymentReceived();
        newOrganizationListPage.clickConfirmFromPopup();
        newOrganizationListPage.checkSuccessToastForUnblockOrganization();
        newOrganizationListPage.checkOrganizationStatus(organizationName,"Live");
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4605")
    @Test
    public void unblockLiveWithOtherReason() {

        final String organizationName = ORGANIZATION_FLOWS.createBlockedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(UNBLOCK);
        newOrganizationListPage.clickOnOtherReason();
        newOrganizationListPage.typeReason(FAKER.harryPotter().spell());
        newOrganizationListPage.clickConfirmFromPopup();
        newOrganizationListPage.checkSuccessToastForUnblockOrganization();
        newOrganizationListPage.checkOrganizationStatus(organizationName,"Live");
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4597")
    @Test
    public void missingOtherReason() {

        final String organizationName = ORGANIZATION_FLOWS.createBlockedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("internalName");
        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(organizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(UNBLOCK);
        newOrganizationListPage.clickOnOtherReason();
        newOrganizationListPage.clickConfirmFromPopup();
        newOrganizationListPage.checkRequiredOtherFieldError();
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4601")
    @Test
    public void cancelButtonFromUnlockPopup() {

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(blockedOrganizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(UNBLOCK);
        newOrganizationListPage.clickCancelFromPopup();
        newOrganizationListPage.checkNoPopupIsDisplayed();
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4608")
    @Test
    public void xButtonFromBlockPopup() {

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(liveOrganizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(BLOCK);
        newOrganizationListPage.clickOnCloseIcon();
        newOrganizationListPage.checkNoPopupIsDisplayed();
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4609")
    @Test
    public void checkUnblockPopup() {

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(blockedOrganizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(UNBLOCK);
        newOrganizationListPage.checkUnblockingPopupContent(blockedOrganizationName);
    }

    @Xray(requirement = "PEG-2046", test = "PEG-4610")
    @Test
    public void checkBlockPopup() {

        final OrganizationListPage organizationListPage = new OrganizationListPage(browserToUse, versionToBe, supportToken);
        organizationListPage.openPage();
        final OrganizationListPage newOrganizationListPage = organizationListPage.searchForOrganization(liveOrganizationName);
        newOrganizationListPage.clickThreeDotItemActionsMenu();
        newOrganizationListPage.clickOnActionMenuItem(BLOCK);
        newOrganizationListPage.checkBlockingPopupContent(liveOrganizationName);
    }

}