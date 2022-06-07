package e2e.gatewayapps.userresource;


import configuration.Role;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.usersresource.payloads.UserUpdateCurrentBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;

public class UserUpdateCurrentTest {

    private String organizationId;
    private String locationId;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;
    private final UserFlows userFlows = new UserFlows();
    private OrganizationFlows organizationFlows;
    private static final Logger LOGGER  = Logger.getLogger(UserUpdateCurrentTest.class);

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
    }

    @Test(testName = "PEG-2773", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void updateUsers(Role role) {
        final String token = role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;

        UserHelper.updateCurrentUser(token,  bodyBuilder())
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-2776", dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void updateUsersWithInvalidPhone(Object phoneNumber) {
        final JSONObject details = bodyBuilder();
        details.put(CONTACT_NUMBER, phoneNumber);
        UserHelper.updateCurrentUser(staffToken, details)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2775")
    public void updateUsersWithEmptyFields() {
        final JSONObject details = bodyBuilder();
        details.remove(FIRST_NAME);
        UserHelper.updateCurrentUser(staffToken, details)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

        details.remove(LAST_NAME);
        details.put(FIRST_NAME, "FirstName " + new Random().nextInt());
        UserHelper.updateCurrentUser(adminToken, details)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

        LOGGER.info("This part of test is checking restriction of phone number removal from contact person");
        details.remove(CONTACT_NUMBER);
        details.put(LAST_NAME, "LastName " + new Random().nextInt());
        UserHelper.updateCurrentUser(ownerToken, details)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-2774")
    public void updateUsersWhenDeactivated() {
        final JSONObject newLocationAdminUser = userFlows.createUser(organizationId, LOCATION_ADMIN, Collections.singletonList(locationId));
        userFlows.inactivateUserById(organizationId, newLocationAdminUser.getString("id"));

        UserHelper.updateCurrentUser(newLocationAdminUser.getString("token"), bodyBuilder())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test(testName = "PEG-2777")
    public void updateUsersWhenDeleted() {
        final JSONObject newAdminUser = userFlows.createUser(organizationId,ADMIN, null);
        userFlows.deleteUser(organizationId, newAdminUser.getString("id"));

        UserHelper.updateCurrentUser(newAdminUser.getString("token"), bodyBuilder())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test(testName = "PEG-2778")
    public void updateUserWithPausedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");

        organizationFlows.pauseOrganization(organizationId);
        final JSONObject details = bodyBuilder();
        UserHelper.updateCurrentUser(ownerToken, details)
                .then()
                .statusCode(SC_OK);
        UserHelper.updateCurrentUser(adminToken, details)
                .then()
                .statusCode(SC_OK);
        UserHelper.updateCurrentUser(locationAdminToken, details)
                .then()
                .statusCode(SC_OK);
        UserHelper.updateCurrentUser(staffToken, details)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-2779")
    public void updateUserWithBlockedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");

        organizationFlows.blockOrganization(organizationId);
        final JSONObject details = bodyBuilder();
        UserHelper.updateCurrentUser(ownerToken, details)
                .then()
                .statusCode(SC_OK);
        UserHelper.updateCurrentUser(adminToken, details)
                .then()
                .statusCode(SC_OK);
        UserHelper.updateCurrentUser(locationAdminToken, details)
                .then()
                .statusCode(SC_OK);
        UserHelper.updateCurrentUser(staffToken, details)
                .then()
                .statusCode(SC_OK);
    }


    @Test(testName = "PEG-2780", priority = 30)
    public void updateUserWithDeletedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");

        organizationFlows.deleteOrganization(organizationId);
        final JSONObject details = bodyBuilder();
        UserHelper.updateCurrentUser(ownerToken, details)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        UserHelper.updateCurrentUser(adminToken, details)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        UserHelper.updateCurrentUser(locationAdminToken, details)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
        UserHelper.updateCurrentUser(staffToken, details)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }
}
