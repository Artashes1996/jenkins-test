package e2e.ui.pages.users;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.DBHelper;
import helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.SignInPage;
import pages.UsersListPage;
import utils.RetryAnalyzer;
import utils.TestUtils;
import utils.Xray;

import java.util.ArrayList;
import java.util.Collections;

import static configuration.Role.OWNER;
import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.*;
import static utils.TestUtils.getRandomInt;

public class UsersListPageTest extends BasePageTest {

    private String organizationId;
    private String organizationIdForLocationsDropDown;
    private String notLinkedLocationName;
    private JSONObject organizationAndOwnerForLocationsDropDown;
    private JSONArray linkedLocationsForLocationsDropDown;
    private JSONArray linkedUsersForLocationsDropDown;
    private JSONArray usersArray;
    private UserFlows userFlows;
    private String locationId;

    @BeforeClass
    void browserSetup() {
        final int invitedUsersCount = 51;
        userFlows = new UserFlows();
        linkedLocationsForLocationsDropDown = new JSONArray();
        linkedUsersForLocationsDropDown = new JSONArray();

        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final LocationFlows locationFlows = new LocationFlows();
        organizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        locationId = locationFlows.createLocation(organizationId).getString("id");
        usersArray = userFlows.inviteUsers(organizationId, Collections.singletonList(locationId), Role.STAFF, invitedUsersCount);
        organizationAndOwnerForLocationsDropDown = organizationFlows.createAndPublishOrganizationWithOwner();
        organizationIdForLocationsDropDown = organizationAndOwnerForLocationsDropDown.getJSONObject("ORGANIZATION").getString("id");
        linkedLocationsForLocationsDropDown = locationFlows.createLocations(organizationIdForLocationsDropDown, 2);

        notLinkedLocationName = locationFlows.createLocation(organizationIdForLocationsDropDown).getString("internalName");
        linkedUsersForLocationsDropDown.put(
                userFlows.createUser(organizationIdForLocationsDropDown, Role.LOCATION_ADMIN, Collections.singletonList(linkedLocationsForLocationsDropDown.getJSONObject(0).getString("id")))
        );
        linkedUsersForLocationsDropDown.put(
                userFlows.createUser(organizationIdForLocationsDropDown, Role.ADMIN, Collections.singletonList(linkedLocationsForLocationsDropDown.getJSONObject(1).getString("id")))
        );
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-2231", test = "PEG-3283")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkPagination() {
        final UsersListPage usersList = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersList.openPage();
        usersList.checkUsersCount(userFlows.getNumberOfUsersCount(organizationId));
    }

    @Xray(requirement = "PEG-2231", test = "PEG-2900")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkSortByUserName() {
        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.checkIfUserNamesColumnIsAscendingSorted();
        usersListPage.clickOnSortByUserNameButton();
        usersListPage.checkIfUserNamesColumnIsDescendingSorted();
        usersListPage.clickOnSortByUserNameButton();
        usersListPage.checkIfUserNamesColumnIsAscendingSorted();
    }

    @Xray(requirement = "PEG-2231", test = "PEG-2896")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkSearchUsers() {
        final UsersListPage usersList = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersList.openPage();
        final JSONObject jsonObject = SearchOrganizationRequestBody.bodyBuilder(SearchOrganizationRequestBody.OrganizationSearchCombination.WITH_FULL_PAGINATION);
        jsonObject.getJSONObject(PAGINATION).put(SIZE, 50);
        jsonObject.getJSONObject(PAGINATION).put(SORT, "FIRST_NAME:ASC");
        final JSONObject user = usersArray.getJSONObject(getRandomInt(usersArray.length() - 1));
        usersList.fillValueInSearchUsersField(user.getString("firstName"));
        usersList.checkUserInList(user);
    }

    @Xray(requirement = "PEG-2661", test = "PEG-3523")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkUserStatusColorWhenUserIsDeleted() {
        final JSONObject userObject = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String userId = userObject.getString("id");
        final String userName = userObject.getString("firstName") + " " + userObject.getString("lastName");
        userFlows.deleteUser(organizationId, userId);
        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.fillValueInSearchUsersField(userName);
        usersListPage.checkUserStatusWithColor(UsersListPage.STATUS.DELETED);
    }

    @Xray(requirement = "PEG-2661", test = "PEG-3524")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkUserStatusColorWhenUserStatusIsInactive() {
        final JSONObject userObject = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String userId = userObject.getString("id");
        final String userName = userObject.getString("firstName") + " " + userObject.getString("lastName");
        userFlows.inactivateUserById(organizationId, userId);
        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.fillValueInSearchUsersField(userName);
        usersListPage.checkUserStatusWithColor(UsersListPage.STATUS.INACTIVE);
    }

    @Xray(requirement = "PEG-2661", test = "PEG-3526")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkUserStatusColorWhenUserStatusIsExpired() {
        final JSONObject userObject = userFlows.inviteUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String token = userObject.getString("token");
        final String userName = userObject.getString("firstName") + " " + userObject.getString("lastName");
        DBHelper.expireInvitationToken(token);
        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.fillValueInSearchUsersField(userName);
        usersListPage.checkUserStatusWithColor(UsersListPage.STATUS.EXPIRED);
    }

    @Xray(requirement = "PEG-2661", test = "PEG-3527")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkUserStatusColorWhenUserStatusIsPending() {
        final JSONObject userObject = userFlows.inviteUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String userName = userObject.getString("firstName") + " " + userObject.getString("lastName");
        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.fillValueInSearchUsersField(userName);
        usersListPage.checkUserStatusWithColor(UsersListPage.STATUS.PENDING);
    }

    @Xray(requirement = "PEG-2661", test = "PEG-3528")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkUserStatusColorWhenUserStatusIsActive() {
        final JSONObject userObject = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId));
        final String userName = userObject.getString("firstName") + " " + userObject.getString("lastName");
        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.fillValueInSearchUsersField(userName);
        usersListPage.checkUserStatusWithColor(UsersListPage.STATUS.ACTIVE);
    }

    @Xray(requirement = "PEG-2984", test = "PEG-4673")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkSelectAllLocationsDropDownOption() {

        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationIdForLocationsDropDown, supportToken);
        usersListPage.openPage();
        usersListPage.clickOnLocationDropDown();
        usersListPage.clickOnSelectAllOptionFromLocationsDropDown();
        linkedUsersForLocationsDropDown.forEach(user -> usersListPage.checkUserInList((JSONObject) user));

    }

    @Xray(requirement = "PEG-2984", test = "PEG-4674")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkEmptyDropDownText() {

        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationId, supportToken);
        usersListPage.openPage();
        usersListPage.clickOnLocationDropDown();
        usersListPage.fillLocationDropDownSearchField(TestUtils.getRandomPhoneNumber());
        usersListPage.checkEmptyLocationDropDownText();

    }

    @Xray(requirement = "PEG-2984", test = "PEG-4675")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkLocationWithoutLinkedUsers() {

        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationIdForLocationsDropDown, supportToken);
        usersListPage.openPage();
        usersListPage.clickOnLocationDropDown();
        usersListPage.fillLocationDropDownSearchField(notLinkedLocationName);
        usersListPage.clickOnOptionFromLocationsDropDownByName(notLinkedLocationName);
        usersListPage.checkEmptyPage();

    }

    @Xray(requirement = "PEG-2984", test = "PEG-4678")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkLocationDropDownSearchClean() {

        final ArrayList<String> locationNames = new ArrayList<>();
        locationNames.add(notLinkedLocationName);
        locationNames.add(linkedLocationsForLocationsDropDown.getJSONObject(0).getString("internalName"));
        locationNames.add(linkedLocationsForLocationsDropDown.getJSONObject(1).getString("internalName"));

        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationIdForLocationsDropDown, supportToken);
        usersListPage.openPage();
        usersListPage.clickOnLocationDropDown();
        usersListPage.checkDropDownItems(locationNames);
        usersListPage.fillLocationDropDownSearchField(notLinkedLocationName);
        usersListPage.checkDropDownItems(Collections.singletonList(notLinkedLocationName));
        usersListPage.clearLocationDropDownSearchField();
        usersListPage.checkDropDownItems(locationNames);

    }

    @Xray(requirement = "PEG-2984", test = "PEG-4676")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void checkTwoLocationOptionsCheck() {

        final UsersListPage usersListPage = new UsersListPage(browserToUse, versionToBe, organizationIdForLocationsDropDown, supportToken);
        usersListPage.openPage();
        usersListPage.clickOnLocationDropDown();
        usersListPage.clickOnOptionFromLocationsDropDownByName(linkedLocationsForLocationsDropDown.getJSONObject(0).getString("internalName"));
        usersListPage.clickOnOptionFromLocationsDropDownByName(linkedLocationsForLocationsDropDown.getJSONObject(1).getString("internalName"));

        linkedUsersForLocationsDropDown.forEach(user -> usersListPage.checkUserInList((JSONObject) user));
        usersListPage.checkUserIsNotInTheList(organizationAndOwnerForLocationsDropDown.getJSONObject(OWNER.name()));

    }
}


