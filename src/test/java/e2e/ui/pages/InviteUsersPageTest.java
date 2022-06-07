package e2e.ui.pages;

import configuration.Role;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.InvitationCreationPage;
import pages.SignInPage;
import pages.UsersListPage;
import utils.Xray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static utils.TestUtils.getRandomInt;

public class InviteUsersPageTest extends BasePageTest {
    private String browserToUse;
    private String versionToBe;
    private String supportToken;
    private String organizationId;
    private JSONObject organizationAndUsers;


    @Parameters({"browser", "version"})
    @BeforeClass
    protected void browserSetup(String browserName, String version) {
        browserToUse = browserName;
        versionToBe = version;
        supportToken = new AuthenticationFlowHelper().getToken(Role.SUPPORT);
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
        organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @Xray(requirement = "PEG-2361", test = "PEG-3015")
    @Test
    public void checkErrorMessagesWhenAddingUsers(){
        final InvitationCreationPage inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, organizationId, supportToken);
        inviteUsersPage.openPage();
        final List<String> duplicateEmails = Arrays.asList("user1@user.qa", "user1@user.qa");
        inviteUsersPage.checkAddButtonToBeDisabled();
        inviteUsersPage.addUsers(duplicateEmails);
        inviteUsersPage.checkDuplicateEmailsErrorMessage();
        final List<String> wrongFormattedEmail = Arrays.asList("user1@user.qa", "user1user.qa");
        inviteUsersPage.checkAddButtonToBeDisabled();
        inviteUsersPage.addUsers(wrongFormattedEmail);
        inviteUsersPage.checkWrongEmailFormatErrorMessage();
        final List<String> moreThanTwentyEmails = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            moreThanTwentyEmails.add(getRandomInt() + "@user.qa");
        }
        inviteUsersPage.checkAddButtonToBeDisabled();
        inviteUsersPage.addUsers(moreThanTwentyEmails);
        inviteUsersPage.checkMoreThanTwentyUsersErrorMessage();
    }

    @Xray(requirement = "PEG-2361", test = "PEG-3016")
    @Test
    public void checkErrorMessagesWhenInvitingUsers(){
        final InvitationCreationPage inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, organizationId, supportToken);
        inviteUsersPage.openPage();
        final UserFlows userFlows = new UserFlows();
        final String acceptedUserEmail = userFlows.createUser(organizationId, Role.ADMIN, null).getString("email");
        final JSONObject deletedUser = userFlows.createUser(organizationId, Role.OWNER, null);
        final String deletedUserEmail = deletedUser.getString("email");
        userFlows.deleteUser(organizationId, deletedUser.getString("id"));
        final String invitedUserEmail = userFlows.inviteUser(organizationId, Role.ADMIN, null).getString("email");
        final String otherOrganizationUserEmail = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject(Role.OWNER.name()).getString("email");
        final List<String> users = Arrays.asList(otherOrganizationUserEmail, invitedUserEmail, deletedUserEmail, acceptedUserEmail);

        inviteUsersPage.addUsers(users);
        inviteUsersPage.checkInvitationCount(users.size());
        inviteUsersPage.inviteUsers();
        inviteUsersPage.checkErrorPopupOtherOrganizationUsersSupport(Collections.singletonList(otherOrganizationUserEmail));
        inviteUsersPage.removeUserFromInvitationList(0);
        inviteUsersPage.checkInvitationCount(users.size()-1);
        inviteUsersPage.inviteUsers();
        inviteUsersPage.checkErrorPopupAlreadyInvitedUsers(Collections.singletonList(invitedUserEmail));
        inviteUsersPage.removeUserFromInvitationList(0);
        inviteUsersPage.checkInvitationCount(users.size()-2);
        inviteUsersPage.inviteUsers();
        inviteUsersPage.checkErrorPopupDeletedUsers(Collections.singletonList(deletedUserEmail));
        inviteUsersPage.removeUserFromInvitationList(0);
        inviteUsersPage.checkInvitationCount(users.size()-3);
        inviteUsersPage.inviteUsers();
        inviteUsersPage.checkErrorPopupExistingUsers(Collections.singletonList(acceptedUserEmail));
    }

    @Xray(requirement = "PEG-2397", test = "PEG-3017")
    @Test
    public void inviteSeveralUsersAndCheckInTheList() {
        final LocationFlows locationFlows = new LocationFlows();
        locationFlows.createLocation(organizationId);

        final JSONObject allUsers = new JSONObject();
        final JSONObject owner = new JSONObject();
        owner.put("email", getRandomInt() + "owner@qless.com");
        owner.put("firstname", "1 Owner");
        owner.put("lastname", "User");
        owner.put("phoneNumber", "+37455911828");
        owner.put("role", Role.OWNER);
        final JSONObject admin = new JSONObject();
        admin.put("email", getRandomInt() + "admin@qless.com");
        admin.put("firstname", "2 Admin");
        admin.put("lastname", "User");
        admin.put("phoneNumber", "+37455911828");
        admin.put("role", Role.ADMIN);
        final JSONObject locationAdmin = new JSONObject();
        locationAdmin.put("email", getRandomInt() + "locationadmin@qless.com");
        locationAdmin.put("firstname", "3 Location Admin");
        locationAdmin.put("lastname", "User");
        locationAdmin.put("phoneNumber", "+37455911828");
        locationAdmin.put("role", Role.LOCATION_ADMIN);
        final JSONObject staff = new JSONObject();
        staff.put("email", getRandomInt() + "staff@qless.com");
        staff.put("firstname", "4 Staff");
        staff.put("lastname", "User");
        staff.put("phoneNumber", "+37455911828");
        staff.put("role", Role.STAFF);

        allUsers.put(Role.OWNER.name(), owner);
        allUsers.put(Role.ADMIN.name(), admin);
        allUsers.put(Role.LOCATION_ADMIN.name(), locationAdmin);
        allUsers.put(Role.STAFF.name(), staff);

        final InvitationCreationPage inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, organizationId, supportToken);
        inviteUsersPage.openPage();
        final List<String> emails = Arrays.asList(allUsers.getJSONObject(Role.OWNER.name()).getString("email"),
                allUsers.getJSONObject(Role.ADMIN.name()).getString("email"),
                allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("email"),
                allUsers.getJSONObject(Role.STAFF.name()).getString("email"));
        inviteUsersPage.addUsers(emails);
        inviteUsersPage.checkEmailOrderAfterAdding(emails);
        inviteUsersPage.checkRoleDropdownValues(Arrays.asList(Role.OWNER.name(), Role.ADMIN.name(), Role.LOCATION_ADMIN.name(), Role.STAFF.name()));
        inviteUsersPage.fillAllFieldsWithAllLocations(0, allUsers.getJSONObject(Role.OWNER.name()).getString("firstname"),
                allUsers.getJSONObject(Role.OWNER.name()).getString("lastname"),
                allUsers.getJSONObject(Role.OWNER.name()).getString("phoneNumber"), Role.OWNER);

        inviteUsersPage.fillAllFields(1, allUsers.getJSONObject(Role.ADMIN.name()).getString("firstname"),
                allUsers.getJSONObject(Role.ADMIN.name()).getString("lastname"),
                allUsers.getJSONObject(Role.ADMIN.name()).getString("phoneNumber"), Role.ADMIN);

        inviteUsersPage.fillAllFieldsWithAllLocations(2, allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("firstname"),
                allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("lastname"),
                allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("phoneNumber"), Role.LOCATION_ADMIN);

        inviteUsersPage.fillAllFields(3, allUsers.getJSONObject(Role.STAFF.name()).getString("firstname"),
                allUsers.getJSONObject(Role.STAFF.name()).getString("lastname"),
                allUsers.getJSONObject(Role.STAFF.name()).getString("phoneNumber"), Role.STAFF, 1);

        final UsersListPage usersList = inviteUsersPage.inviteUsers();

        usersList.checkSuccessToastMessage(4);
        usersList.checkUserInList(0, allUsers.getJSONObject(Role.OWNER.name()).getString("firstname") + " " + allUsers.getJSONObject(Role.OWNER.name()).getString("lastname"),
                Role.OWNER.name(), allUsers.getJSONObject(Role.OWNER.name()).getString("email"), "active");
        usersList.checkUserInList(1, allUsers.getJSONObject(Role.ADMIN.name()).getString("firstname") + " " + allUsers.getJSONObject(Role.ADMIN.name()).getString("lastname"),
                Role.ADMIN.name(), allUsers.getJSONObject(Role.ADMIN.name()).getString("email"), "active");
        usersList.checkUserInList(2, allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("firstname") + " " + allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("lastname"),
                Role.LOCATION_ADMIN.name(), allUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("email"), "active");
        usersList.checkUserInList(3, allUsers.getJSONObject(Role.STAFF.name()).getString("firstname") + " " + allUsers.getJSONObject(Role.STAFF.name()).getString("lastname"),
                Role.STAFF.name(), allUsers.getJSONObject(Role.STAFF.name()).getString("email"), "active");
    }

    @Xray(requirement = "PEG-2397", test = "PEG-3018")
    @Test
    public void checkRoleDropdownValues(){
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(Role.OWNER.name()).getString("email"));
        InvitationCreationPage inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, ownerToken);
        inviteUsersPage.openPage();
        inviteUsersPage.addUsers(Collections.singletonList(getRandomInt()+"@user.qa"));
        inviteUsersPage.checkRoleDropdownValues(Arrays.asList(Role.OWNER.name(), Role.ADMIN.name(), Role.LOCATION_ADMIN.name(), Role.STAFF.name()));

        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(Role.ADMIN.name()).getString("email"));
        inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, adminToken);
        inviteUsersPage.addUsers(Collections.singletonList(getRandomInt()+"@user.qa"));
        inviteUsersPage.checkRoleDropdownValues(Arrays.asList(Role.LOCATION_ADMIN.name(), Role.STAFF.name()));

        final String locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(Role.LOCATION_ADMIN.name()).getString("email"));
        inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, locationAdminToken);
        inviteUsersPage.addUsers(Collections.singletonList(getRandomInt()+"@user.qa"));
        inviteUsersPage.checkRoleDropdownValues(Collections.singletonList(Role.STAFF.name()));
    }

    @Xray(requirement = "PEG-2397", test = "PEG-3019")
    @Test
    public void checkCancelButton(){

        final String organizationId = new OrganizationFlows().createUnpublishedOrganization().getString("id");
        final InvitationCreationPage inviteUsersPage = new InvitationCreationPage(browserToUse, versionToBe, organizationId, supportToken);
        inviteUsersPage.openPage();
        final List<String> emails = Collections.singletonList(getRandomInt()+"owner@qless.com");
        inviteUsersPage.addUsers(emails);
        inviteUsersPage.addUsers(emails);
        inviteUsersPage.checkDuplicateEmailsErrorMessage();
        final UsersListPage usersList = inviteUsersPage.cancelInvitations();
        usersList.checkEmptyPage();
    }

}
