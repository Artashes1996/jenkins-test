package e2e.ui.pages.users;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.DBHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.SignInPage;
import pages.UserDetailsPageEditModePage;
import pages.UserDetailsPageViewModePage;
import utils.Xray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utils.TestUtils.*;

public class UserDetailsPageTest extends BasePageTest {

    private UserFlows userFlows;
    private LocationFlows locationFlows;
    private ThreadLocal<JSONObject> organizationAndUsers;
    private ThreadLocal<String> organizationId;

    @BeforeClass
    public final void setup(){
        organizationAndUsers = new ThreadLocal<>();
        organizationId = new ThreadLocal<>();
    }

    @BeforeMethod
    public final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
        organizationAndUsers.set(new OrganizationFlows().createAndPublishOrganizationWithAllUsers());
        organizationId.set(organizationAndUsers.get().getJSONObject("ORGANIZATION").getString("id"));
        userFlows = new UserFlows();
        locationFlows = new LocationFlows();
    }

    @Xray(requirement = "PEG-2504", test = "PEG-3313, PEG-3314")
    @Test
    public void userDetailsPageTest() {
        final UserDetailsPageViewModePage userDetailsPageViewMode = new UserDetailsPageViewModePage(browserToUse, versionToBe, organizationId.get(), supportToken);
        userDetailsPageViewMode.userToEnter.set(userFlows.createUserWithoutPOC(organizationId.get(), Role.LOCATION_ADMIN,
                Collections.singletonList(organizationAndUsers.get().getJSONObject("LOCATION").getString("id"))));
        userDetailsPageViewMode.openPage();

        userDetailsPageViewMode.checkUserDetails(userDetailsPageViewMode.userToEnter.get());

        final JSONObject userEditBody = new JSONObject();
        userEditBody.put("firstName", FAKER.name().firstName());
        userEditBody.put("lastName", FAKER.name().lastName());
        userEditBody.put("contactNumber", getRandomPhoneNumber());
        userEditBody.put("email", userDetailsPageViewMode.userToEnter.get().getString("email"));
        userEditBody.put("role", userDetailsPageViewMode.userToEnter.get().getString("role"));

        final UserDetailsPageEditModePage userDetailsPageEditMode = new UserDetailsPageEditModePage(browserToUse, versionToBe);
        userDetailsPageEditMode.userToEnter.set(userDetailsPageViewMode.userToEnter.get());
        userDetailsPageEditMode.openPage();

        userDetailsPageEditMode.checkRequiredFields();
        userDetailsPageEditMode.editUserDetails(userEditBody);

        userDetailsPageViewMode.checkUserDetails(userEditBody);
        userDetailsPageViewMode.checkContactPerson();
    }

    @Xray(requirement = "PEG-2453", test = "PEG-3773")
    @Test
    public void changeUserStatus() {
        final UserDetailsPageEditModePage userDetailsPageEditMode = new UserDetailsPageEditModePage(browserToUse, versionToBe, organizationId.get(), supportToken);
        userDetailsPageEditMode.userToEnter.set(userFlows.createUserWithoutPOC(organizationId.get(), Role.OWNER,
                Collections.singletonList(organizationAndUsers.get().getJSONObject("LOCATION").getString("id"))));
        userDetailsPageEditMode.openPage();
        userDetailsPageEditMode.changeUserStatus();

        final UserDetailsPageViewModePage userDetailsPageViewMode = new UserDetailsPageViewModePage(browserToUse, versionToBe);
        userDetailsPageViewMode.userToEnter.set(userDetailsPageEditMode.userToEnter.get());
        userDetailsPageViewMode.openPage();
        userDetailsPageViewMode.checkUserStatusIs("Inactive");

        userDetailsPageEditMode.openPage();
        userDetailsPageEditMode.changeUserStatus();

        userDetailsPageViewMode.checkUserStatusIs("Active");
    }

    @Xray(requirement = "PEG-2453", test = "PEG-3776")
    @Test
    public void changeUserRoleOwnerToStaff() {
        final UserDetailsPageEditModePage userDetailsPageEditMode = new UserDetailsPageEditModePage(browserToUse, versionToBe, organizationId.get(), supportToken);

        userDetailsPageEditMode.userToEnter.set(userFlows.inviteUserWithoutPOC(organizationId.get(), Role.OWNER, null));
        final int virtualAndPhysicalLocationsCount = 3;
        final JSONArray locations = locationFlows
                .createLocations(organizationId.get(), virtualAndPhysicalLocationsCount + virtualAndPhysicalLocationsCount);
        locations.put(organizationAndUsers.get().getJSONObject("LOCATION"));
        final String roleToChange = "Staff";
        userDetailsPageEditMode.openPage();
        userDetailsPageEditMode.changeUserRoleTo(roleToChange, null);
        userDetailsPageEditMode.checkLocationErrorMessage();
        userDetailsPageEditMode.changeUserRoleTo(roleToChange, locations);
        final UserDetailsPageViewModePage userDetailsPageViewMode = new UserDetailsPageViewModePage(browserToUse, versionToBe);
        userDetailsPageViewMode.userToEnter.set(userDetailsPageEditMode.userToEnter.get());
        userDetailsPageViewMode.checkRole(roleToChange);
    }

    @Xray(requirement = "PEG-2453", test = "3775")
    @Test
    public void resendInvitation() {
        final UserDetailsPageViewModePage userDetailsPageViewMode = new UserDetailsPageViewModePage(browserToUse, versionToBe, organizationId.get(), supportToken);
        userDetailsPageViewMode.userToEnter.set(userFlows.inviteUserWithoutPOC(organizationId.get(), Role.OWNER,
                Collections.singletonList(organizationAndUsers.get().getJSONObject("LOCATION").getString("id"))));
        final String userId = userDetailsPageViewMode.userToEnter.get().getString("id");
        DBHelper.expireInvitationByUserId(userId);
        userDetailsPageViewMode.openPage();
        userDetailsPageViewMode.checkUserStatusIs("Expired");
        userDetailsPageViewMode.resendInvitation();
        userDetailsPageViewMode.checkUserStatusIs("Pending");
    }

    @Xray(requirement = "PEG-2453", test = "PEG-4569")
    @Test
    public void removeCommonLocationFromStaffByLocationAdmin() {
        final JSONObject commonLocation = organizationAndUsers.get().getJSONObject("LOCATION");
        final String locationAdminToken = organizationAndUsers.get().getJSONObject(Role.LOCATION_ADMIN.name()).getString("token");
        final int physicalAndVirtualLocationsCount = 2;
        final JSONArray locations = locationFlows.createLocations(organizationId.get(), physicalAndVirtualLocationsCount + physicalAndVirtualLocationsCount);
        locations.put(commonLocation);
        final List<String> locationIds = new ArrayList<>();
        locations.forEach(location -> locationIds.add(((JSONObject) location).getString("id")));
        final UserDetailsPageEditModePage userDetailsPageEditMode = new UserDetailsPageEditModePage(browserToUse, versionToBe, locationAdminToken);
        userDetailsPageEditMode.userToEnter.set(userFlows.createUser(organizationId.get(), Role.STAFF, locationIds));
        userDetailsPageEditMode.openPage();
        userDetailsPageEditMode.removeCommonLocations();

        final UserDetailsPageViewModePage userDetailsPageViewMode = new UserDetailsPageViewModePage(browserToUse, versionToBe, locationAdminToken);
        userDetailsPageViewMode.userToEnter.set(userDetailsPageEditMode.userToEnter.get());
        userDetailsPageViewMode.openPage();
        userDetailsPageViewMode.checkCannotEdit();
    }

    @Xray(test = "PEG-5610", requirement = "PEG-4524")
    @Test
    public void breadCrumbViewAndFunctionality() {
        final UserDetailsPageEditModePage userDetailsPageEditMode = new UserDetailsPageEditModePage(browserToUse, versionToBe, organizationId.get(), supportToken);
        final String locationId = organizationAndUsers.get().getJSONObject("LOCATION").getString("id");
        userDetailsPageEditMode.userToEnter.set(userFlows.createUser(organizationId.get(), Role.OWNER, Collections.singletonList(locationId)));
        final UserDetailsPageViewModePage userDetailsPageViewMode = new UserDetailsPageViewModePage(browserToUse, versionToBe, organizationId.get(), supportToken);
        userDetailsPageViewMode.userToEnter.set(userDetailsPageEditMode.userToEnter.get());
        userDetailsPageViewMode.openPage();
        userDetailsPageViewMode.checkBreadcrumb(userDetailsPageEditMode.userToEnter.get());
    }

}
