package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.gatewayapps.userresource.data.UserDetailsDataProvider;
import helpers.DBHelper;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.organizationsresource.OrganizationsHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.appsapi.usersresource.payloads.UserUpdateBody;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import helpers.appsapi.usersresource.UserHelper;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.*;
import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.PointsOfContacts.*;
import static org.apache.http.HttpStatus.*;
import static configuration.Role.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;


public class UserUpdateTest extends BaseTest {

    private String organizationId;
    private String locationId;
    private String otherLocationId;
    private JSONObject owner;
    private JSONObject admin;
    private JSONObject locationAdmin;
    private JSONObject staff;
    private JSONObject organizationAndUsers;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;
    private UserFlows userFlows;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;

    private ArrayList<String> responsibilityAreas;


    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        owner = organizationAndUsers.getJSONObject(OWNER.name());
        admin = organizationAndUsers.getJSONObject(ADMIN.name());
        locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToken = owner.getString("token");
        adminToken = admin.getString("token");
        locationAdminToken = locationAdmin.getString("token");
        staffToken = staff.getString("token");

        responsibilityAreas = new ArrayList<>();
        responsibilityAreas.add(ADMINISTRATIVE.name());
        responsibilityAreas.add(BILLING.name());
        responsibilityAreas.add(OTHER.name());
        responsibilityAreas.add(TECHNICAL.name());

        otherLocationId = new LocationFlows().createLocation(organizationId).getString("id");
    }

    @Xray(test = "PEG-579", requirement = "PEG-125")
    @Test(testName = "PEG-579", dataProvider = "invalidName", dataProviderClass = UserDetailsDataProvider.class)
    public void updateWithInvalidFirstName(Object firstName) {
        final JSONObject newUser = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId));
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, newUser.getString("id"));
        updateBody.put(UserUpdateBody.FIRST_NAME, firstName);

        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-580", requirement = "PEG-125")
    @Test(testName = "PEG-580", dataProvider = "invalidName", dataProviderClass = UserDetailsDataProvider.class)
    public void updateWithInvalidLastName(Object lastName) {
        final JSONObject newUser = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId));
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, newUser.getString("id"));
        updateBody.put(UserUpdateBody.LAST_NAME, lastName);

        UserHelper.updateUser(ownerToken, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-582", requirement = "PEG-125")
    @Test(testName = "PEG-582", dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void updateWithInvalidContactNumber(Object phoneNumber) {
        final JSONObject newUser = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId));
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, newUser.getString("id"));
        updateBody.put(UserUpdateBody.CONTACT_NUMBER, phoneNumber);

        UserHelper.updateUser(adminToken, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-581", requirement = "PEG-125")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void updateWithInvalidId(Role role) {
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, staff.getString("id"));
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : locationAdminToken;

        UserHelper.updateUser(token, organizationId, null, updateBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-583", requirement = "PEG-125")
    @Test(dataProvider = "validPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void updateWithValidContactNumber(Object phoneNumber) {
        final JSONObject newUser = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId));
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, newUser.getString("id"));
        updateBody.put(UserUpdateBody.CONTACT_NUMBER, phoneNumber);

        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_OK)
                .body(UserUpdateBody.CONTACT_NUMBER, equalTo(phoneNumber));
        UserHelper.updateUser(ownerToken, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_OK)
                .body(UserUpdateBody.CONTACT_NUMBER, equalTo(phoneNumber));
        UserHelper.updateUser(adminToken, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_OK)
                .body(UserUpdateBody.CONTACT_NUMBER, equalTo(phoneNumber));
        UserHelper.updateUser(locationAdminToken, organizationId, newUser.getString("id"), updateBody)
                .then()
                .statusCode(SC_OK)
                .body(UserUpdateBody.CONTACT_NUMBER, equalTo(phoneNumber));
    }

    @Xray(test = "PEG-596", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateUsersWithSupport(Role role) {
        final String userId = role.equals(OWNER) ? owner.getString("id") : role.equals(ADMIN) ? admin.getString("id") :
                role.equals(LOCATION_ADMIN) ? locationAdmin.getString("id") : staff.getString("id");

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3251", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateValidUsersWithOwner(Role role) {
        final String ownerEmail = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId)).getString("email");
        final String userId = role.equals(OWNER) ? owner.getString("id") : role.equals(ADMIN) ? admin.getString("id") :
                role.equals(LOCATION_ADMIN) ? locationAdmin.getString("id") : staff.getString("id");

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        UserHelper.updateUser(new AuthenticationFlowHelper().getTokenWithEmail(ownerEmail), organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3252", requirement = "PEG-125")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void updateValidUsersWithAdmin(Role role) {
        final String userId = role.equals(LOCATION_ADMIN) ? locationAdmin.getString("id") : staff.getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        UserHelper.updateUser(adminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3254", requirement = "PEG-125")
    @Test(dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void updateUserWithNoAccessWithAdmin(Role role) {
        final String adminEmail = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(locationId)).getString("email");
        final String userId = role.equals(OWNER) ? owner.getString("id") : admin.getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        UserHelper.updateUser(new AuthenticationFlowHelper().getTokenWithEmail(adminEmail), organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3253", requirement = "PEG-125")
    @Test
    public void updateValidUserWithLocationAdmin() {
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, staff.getString("id"));
        UserHelper.updateUser(locationAdminToken, organizationId, staff.getString("id"), updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3255", requirement = "PEG-125")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void updateUserWithNoAccessWithLocationAdmin(Role role) {
        final String locationAdminEmail = userFlows.createUser(organizationId, LOCATION_ADMIN, Collections.singletonList(locationId)).getString("email");

        final String userId = role.equals(OWNER) ? owner.getString("id") : role.equals(ADMIN) ? admin.getString("id") : locationAdmin.getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        UserHelper.updateUser(new AuthenticationFlowHelper().getTokenWithEmail(locationAdminEmail), organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3256", requirement = "PEG-125")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void updateInvalidUsersWithStaff(Role role) {
        final String staffToken = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("token");
        final String userId = role.equals(OWNER) ? owner.getString("id") : role.equals(ADMIN) ? admin.getString("id")
                : locationAdmin.getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        UserHelper.updateUser(staffToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-584", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateAccountWithPendingInvitation(Role role) {
        final String userEmail = userFlows.inviteUser(organizationId, role, Collections.singletonList(locationId)).getString("email");
        final String userId = userFlows.getUserId(userEmail, organizationId);
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        UserHelper.updateUser(ownerToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-585", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateAccountWithExpiredInvitation(Role role) {
        final JSONObject user = userFlows.inviteUser(organizationId, role, Collections.singletonList(locationId));
        final String userId = userFlows.getUserId(user.getString("email"), organizationId);
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        DBHelper.expireInvitationToken(user.getString("token"));

        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-598", requirement = "PEG-125")
    @Test(dataProvider = "invalidAccountStatus", dataProviderClass = UserDetailsDataProvider.class)
    public void updateWithInvalidStatus(Object status) {
        final String newUserId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, newUserId);
        updateBody.put(UserUpdateBody.USER_STATUS, status);

        UserHelper.updateUser(locationAdminToken, organizationId, newUserId, updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(test = "PEG-598", requirement = "PEG-125")
    @Test
    public void updateWithInvalidStatusValue() {
        final String newUserId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, newUserId);
        updateBody.put(UserUpdateBody.USER_STATUS, JSONObject.NULL);

        UserHelper.updateUser(locationAdminToken, organizationId, newUserId, updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }


    @Xray(test = "PEG-599", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void inactivateAllUsersBySupport(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3248", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void inactivateAllUsersByOwner(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(ownerToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3245", requirement = "PEG-125")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void inactivateAllUsersByAdmin(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(adminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3249", requirement = "PEG-125")
    @Test
    public void inactivateAllUsersByLocationAdmin() {
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        updateBody.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(adminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

    // TODO check this test
    @Xray(test = "PEG-1680", requirement = "PEG-125")
    @Test(enabled = false)
    public void removeContactNumberOfUserWithLocationPointOfContact() {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createOwnerWithOrganizationIdWithLocationPOC(organizationId, locationId).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        updateBody.put(UserUpdateBody.LOCATION_LEVEL_POINTS_OF_CONTACT, new JSONObject().put(locationId, responsibilityAreas));
        updateBody.remove(UserUpdateBody.CONTACT_NUMBER);

        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("RESOURCE_NOT_FOUND"));
        final int numberOfAllPOCs = 2;

        OrganizationsHelper.getOrganizationById(SUPPORT_TOKEN, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(numberOfAllPOCs));

        LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(1));
    }

    @Xray(test = "PEG-1681", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void removeContactNumberOfUserWithOrganizationPointOfContact(Role role) {
        final JSONObject user = role.equals(OWNER) ? owner : role.equals(ADMIN) ? admin : role.equals(LOCATION_ADMIN) ? locationAdmin : staff;
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, user.getString("id"));
        final String existingPOC = UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("pointsOfContact.responsibilityArea[0]");

        updateBody.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, responsibilityAreas);
        updateBody.remove(CONTACT_NUMBER);

        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, user.getString("id"), updateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

        UserHelper.getUserFullDetailsById(SUPPORT_TOKEN, organizationId, user.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(1))
                .body("pointsOfContact.responsibilityArea", hasItem(existingPOC));
    }

    @Xray(test = "PEG-1682", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateAndRemoveLocationAndOrganizationPointsOfContactsBySupport(Role role) {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, responsibilityAreas);
        updateBody.put(LOCATION_LEVEL_POINT_OF_CONTACT, new JSONObject().put(locationId, responsibilityAreas));
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(SUPPORT_TOKEN, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size() + 1));
        LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size()));

        updateBody.remove(UserUpdateBody.ORGANIZATION_LEVEL_POINTS_OF_CONTACT);
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(SUPPORT_TOKEN, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(1));
    }

    @Xray(test = "PEG-3257", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateLocationAndOrganizationPointsOfContactsByOwner(Role role) {
        final JSONObject organizationAndOwner = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndOwner.getJSONObject(OWNER.name()).getString("email"));

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, responsibilityAreas);
        updateBody.put(LOCATION_LEVEL_POINT_OF_CONTACT, new JSONObject().put(locationId, responsibilityAreas));
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(ownerToken, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size() + 1));
        LocationsHelper.getLocation(ownerToken, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size()));

        updateBody.remove(UserUpdateBody.ORGANIZATION_LEVEL_POINTS_OF_CONTACT);
        UserHelper.updateUser(ownerToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(ownerToken, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(1));
    }

    @Xray(test = "PEG-3258", requirement = "PEG-125")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void updateLocationAndOrganizationPointsOfContactsByAdmin(Role role) {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final String adminEmail = userFlows.createUserWithoutPOC(organizationId, ADMIN, null).getString("email");
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(adminEmail);

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, responsibilityAreas);
        updateBody.put(LOCATION_LEVEL_POINT_OF_CONTACT, new JSONObject().put(locationId, responsibilityAreas));
        UserHelper.updateUser(adminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(adminToken, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size() + 1));
        LocationsHelper.getLocation(adminToken, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size()));

        updateBody.remove(UserUpdateBody.ORGANIZATION_LEVEL_POINTS_OF_CONTACT);
        UserHelper.updateUser(adminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(adminToken, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(1));
    }

    @Xray(test = "PEG-3259", requirement = "PEG-125")
    @Test
    public void updateLocationAndOrganizationPointsOfContactsByLocationAdmin() {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, responsibilityAreas);
        updateBody.put(LOCATION_LEVEL_POINT_OF_CONTACT, new JSONObject().put(locationId, responsibilityAreas));
        final String locationAdminEmail = userFlows.createUserWithoutPOC(organizationId, LOCATION_ADMIN, Collections.singletonList(locationId)).getString("email");
        final String locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(locationAdminEmail);

        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(locationAdminToken, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size() + 1));
        LocationsHelper.getLocation(locationAdminToken, organizationId, locationId)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(responsibilityAreas.size()));

        updateBody.remove(UserUpdateBody.ORGANIZATION_LEVEL_POINTS_OF_CONTACT);
        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
        OrganizationsHelper.getOrganizationById(locationAdminToken, organizationId, true)
                .then()
                .statusCode(SC_OK)
                .body("pointsOfContact.size()", is(1));
    }

    @Xray(test = "PEG-1686", requirement = "PEG-125")
    @Test
    public void updateUserWithOtherOrganizationUser() {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");

        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateBody.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, responsibilityAreas);
        updateBody.put(LOCATION_LEVEL_POINT_OF_CONTACT, new JSONObject().put(locationId, responsibilityAreas));

        UserHelper.updateUser(ownerToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        UserHelper.updateUser(adminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1687", requirement = "PEG-125")
    @Test
    public void updateLocationAndOrganizationPointsOfContactIncorrectEnum() {
        final JSONArray incorrectEnumList = new JSONArray().put("IncorrectEnum");

        final JSONObject updateBody1 = UserUpdateBody.bodyBuilder(organizationId, owner.getString("id"));
        updateBody1.put(ORGANIZATION_LEVEL_POINT_OF_CONTACT, incorrectEnumList);
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, owner.getString("id"), updateBody1)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));

        final JSONObject updateBody2 = UserUpdateBody.bodyBuilder(organizationId, admin.getString("id"));
        updateBody2.put(LOCATION_LEVEL_POINT_OF_CONTACT, new JSONObject().put(locationId, incorrectEnumList));
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, staff.getString("id"), updateBody2)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(test = "PEG-3260", requirement = "PEG-2431")
    @Test(dataProvider = "valid role change from role to role", dataProviderClass = RoleDataProvider.class)
    public void updateUserRole(Role actionByRole, Role roleChangeFrom, Role roleChangeTo) {
        final String token = actionByRole.equals(SUPPORT) ? SUPPORT_TOKEN : actionByRole.equals(OWNER) ? ownerToken : adminToken;

        final String userId = userFlows.createUser(organizationId, roleChangeFrom, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        if (roleChangeTo.equals(LOCATION_ADMIN) || roleChangeTo.equals(STAFF)) {
            updateBody.remove(WORKING_LOCATION_IDS);
            updateBody.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).put(ROLE_INTERNAL_NAME, roleChangeTo).put(LOCATION_ID, locationId);
        } else {
            updateBody.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).put(ROLE_INTERNAL_NAME, roleChangeTo).remove(LOCATION_ID);
            updateBody.put(WORKING_LOCATION_IDS, new JSONArray().put(locationId));
        }
        UserHelper.updateUser(token, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK);
    }

//  Test should be passed after fixing https://qless-cloud.atlassian.net/browse/PEG-7208
    @Xray(test = "PEG-3261", requirement = "PEG-2431")
    @Test
    public void updateUserRoleToSupport() {
        String userId = userFlows.createUser(organizationId, OWNER, Collections.singletonList(locationId)).getString("id");
        JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        final JSONObject roleLocationPayload = new JSONObject().put(ROLE_INTERNAL_NAME, SUPPORT);
        updateBody.put(ROLE_LOCATION_PAYLOADS, new JSONArray().put(roleLocationPayload));
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    // TODO this is known issue - PEG-3285
    @Xray(test = "PEG-3262", requirement = "PEG-2431")
    @Test(dataProvider = "invalid role change from role to role", dataProviderClass = RoleDataProvider.class)
    public void updateInvalidUserRole(Role actionByRole, Role roleChangeFrom, Role roleChangeTo) {
        final String token = actionByRole.equals(ADMIN) ? adminToken : actionByRole.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        final String userId = userFlows.createUser(organizationId, roleChangeFrom, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        if (roleChangeTo.equals(LOCATION_ADMIN) || roleChangeTo.equals(STAFF)) {
            updateBody.remove(WORKING_LOCATION_IDS);
            updateBody.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).put(ROLE_INTERNAL_NAME, roleChangeTo).put(LOCATION_ID, locationId);
        } else {
            updateBody.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).put(ROLE_INTERNAL_NAME, roleChangeTo).remove(LOCATION_ID);
            updateBody.put(WORKING_LOCATION_IDS, new JSONArray().put(locationId));
        }
        UserHelper.updateUser(token, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3263", requirement = "PEG-2431")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateUserLocation(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final JSONObject userUpdateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        if (role.equals(STAFF) || role.equals(LOCATION_ADMIN)) {
            userUpdateBody.getJSONArray(ROLE_LOCATION_PAYLOADS).getJSONObject(0).put(LOCATION_ID, otherLocationId);
        } else {
            userUpdateBody.put(WORKING_LOCATION_IDS, new JSONArray().put(otherLocationId));
        }
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, userUpdateBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3278", requirement = "PEG-2431")
    @Test
    public void updateOtherLocationStaffByLocationAdmin() {
        final String newLocationId = new LocationFlows().createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(newLocationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        final JSONObject roleLocationPayload = new JSONObject().put(ROLE_INTERNAL_NAME, STAFF);
        roleLocationPayload.put(LOCATION_ID, locationId);

        updateBody.put(ROLE_LOCATION_PAYLOADS, new JSONArray().put(roleLocationPayload));
        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1764", requirement = "PEG-125")
    @Test(dataProvider = "organizationLevelInviters", dataProviderClass = RoleDataProvider.class)
    public void deactivatePOC(Role role) {
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);

        final String token = role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : locationAdminToken;
        updateBody.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        updateBody.remove(WORKING_LOCATION_IDS);

        UserHelper.updateUser(token, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo("INACTIVE"));
    }

    @Xray(test = "PEG-1687", requirement = "PEG-125")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateDeletedUser(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateBody = UserUpdateBody.bodyBuilder(organizationId, userId);
        userFlows.deleteUser(organizationId, userId);
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-1767", requirement = "PEG-125")
    @Test
    public void updateAndDeactivatePausedOrganizationUser() {
        final JSONObject organization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organization.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organization.getJSONObject("LOCATION").getString("id");

        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(OWNER.name()).getString("email"));
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(ADMIN.name()).getString("email"));
        final String locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(LOCATION_ADMIN.name()).getString("email"));

        organizationFlows.pauseOrganization(organizationId);
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateAccount = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(ownerToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(adminToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-1768", requirement = "PEG-125")
    @Test
    public void updateAndDeactivateBlockedOrganizationUser() {
        final JSONObject organization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organization.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organization.getJSONObject("LOCATION").getString("id");

        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(OWNER.name()).getString("email"));
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(ADMIN.name()).getString("email"));
        final String locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(LOCATION_ADMIN.name()).getString("email"));

        organizationFlows.blockOrganization(organizationId);
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");
        final JSONObject updateAccount = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(ownerToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(adminToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3281", requirement = "PEG-125")
    @Test
    public void updateAndDeactivateDeletedOrganizationUser() {
        final JSONObject organization = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organization.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organization.getJSONObject("LOCATION").getString("id");

        final String ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(OWNER.name()).getString("email"));
        final String adminToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(ADMIN.name()).getString("email"));
        final String locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(organization.getJSONObject(LOCATION_ADMIN.name()).getString("email"));
        final String userId = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId)).getString("id");

        organizationFlows.deleteOrganization(organizationId);
        final JSONObject updateAccount = UserUpdateBody.bodyBuilder(organizationId, userId);
        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(ownerToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(adminToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(locationAdminToken, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, userId, updateAccount)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-3284", requirement = "PEG-125")
    @Test
    public void deactivateOnlyPOCUser() {
        final JSONObject organization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = organization.getJSONObject("ORGANIZATION").getString("id");

        final String ownerWithPOCId = organization.getJSONObject(OWNER.name()).getString("id");
        final String ownerWithoutPOCEmail = userFlows.createUserWithoutPOC(organizationId, OWNER, null).getString("email");

        final JSONObject updateAccount = UserUpdateBody.bodyBuilder(organizationId, ownerWithPOCId);
        updateAccount.put(UserUpdateBody.USER_STATUS, "INACTIVE");
//
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, ownerWithPOCId, updateAccount)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("ORGANIZATION_SHOULD_HAVE_AT_LEAST_ONE_ACTIVE_POINT_OF_CONTACT"));
        UserHelper.updateUser(new AuthenticationFlowHelper().getTokenWithEmail(ownerWithoutPOCEmail), organizationId, ownerWithPOCId, updateAccount)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("ORGANIZATION_SHOULD_HAVE_AT_LEAST_ONE_ACTIVE_POINT_OF_CONTACT"));
    }

    @Xray(test = "PEG-5253", requirement = "PEG-4707")
    @Test
    public void checkOldTokenIsInvalidInCaseOfInactivateUser() {
        final Role userRole = Role.getRandomOrganizationRole();
        final JSONObject userObject = userFlows.createUser(organizationId, userRole, Collections.singletonList(locationId));
        final String newUserId = userObject.getString("id");
        final String newUserToken = userObject.getString("token");

        userFlows.inactivateUserById(organizationId, newUserId);
        UserHelper.searchForUsers(newUserToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-5258", requirement = "PEG-4707")
    @Test
    public void checkOldTokenIsInvalidInCaseOfRoleChange() {
        final Role userRole = Role.getRandomOrganizationInviterRole();
        final JSONObject userObject = userFlows.createUser(organizationId, userRole, Collections.singletonList(locationId));
        final String newUserId = userObject.getString("id");
        final String newUserToken = userObject.getString("token");
        final JSONObject updateAccountObject = UserUpdateBody.bodyBuilder(organizationId, newUserId);
        updateAccountObject.getJSONArray("roleLocationPayloads").getJSONObject(0).put("roleInternalName", STAFF).put(LOCATION_ID, locationId);
        updateAccountObject.remove("workingLocationIds");
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, newUserId, updateAccountObject)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(newUserToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));

    }

    @Xray(test = "PEG-5259", requirement = "PEG-4707")
    @Test
    public void checkOldTokenIsValidInCaseOfChangeWorkingLocationByOwnerAndAdmin() {
        final Role userRole = Role.getRandomOrganizationAdminRole();
        final JSONObject userObject = userFlows.createUser(organizationId, userRole, Collections.singletonList(locationId));
        final String newUserId = userObject.getString("id");
        final String newUserToken = userObject.getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject updateAccountObject = UserUpdateBody.bodyBuilder(organizationId, newUserId);
        updateAccountObject.getJSONArray("workingLocationIds").put(newLocationId);
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, newUserId, updateAccountObject)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(newUserToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5260", requirement = "PEG-4707")
    @Test
    public void checkOldTokenIsInvalidInCaseOfChangeRoleLocations() {
        final Role userRole = Role.getRandomRolesWithLocation();
        final JSONObject userObject = userFlows.createUser(organizationId, userRole, Collections.singletonList(locationId));
        final String newUserId = userObject.getString("id");
        final String newUserToken = userObject.getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject updateAccountObject = UserUpdateBody.bodyBuilder(organizationId, newUserId);
        updateAccountObject.getJSONArray("roleLocationPayloads").getJSONObject(0).put(LOCATION_ID, newLocationId);
        UserHelper.updateUser(SUPPORT_TOKEN, organizationId, newUserId, updateAccountObject)
                .then()
                .statusCode(SC_OK);
        UserHelper.searchForUsers(newUserToken, organizationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

}
