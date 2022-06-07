package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class SetPreferredLocationTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;

    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;
    
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(test = "PEG-5824", requirement = "PEG-5256")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void setPreferredLocationSupportedUser(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5825", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationUnsupportedUser() {
        UserHelper.setPreferredLocation(SUPPORT_TOKEN, locationId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-5846", requirement = "PEG-5256")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void setPreferredLocationNotLinkedToUser(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-5847", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationInactiveUser() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject inactiveUser = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId));
        final String token = inactiveUser.getString("token");
        final String inactiveUserId = inactiveUser.getString("id");
        userFlows.inactivateUserById(organizationId, inactiveUserId);

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-5848", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationDeletedUser() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject inactiveUser = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId));
        final String token = inactiveUser.getString("token");
        final String inactiveUserId = inactiveUser.getString("id");
        userFlows.deleteUser(organizationId, inactiveUserId);

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-5849", requirement = "PEG-5256")
    @Test
    public void setPreferredInactiveLocation() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String inactiveLocationId = locationFlows.createInactiveLocation(organizationId).getString("id");

        UserHelper.setPreferredLocation(token, inactiveLocationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5850", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationNonWorkingLocation() {
        final Role randomRole = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        final String locationId = locationFlows.createLocation(organizationId).getString("id");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5851", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationBlockedOrganization() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String token = blockedOrganizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String locationId = blockedOrganizationAndUsers.getJSONObject("LOCATION").getString("id");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5852", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationPausedOrganization() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject pausedOrganizationAndUsers = organizationFlows.createPausedOrganizationWithAllUsers();
        final String token = pausedOrganizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String locationId = pausedOrganizationAndUsers.getJSONObject("LOCATION").getString("id");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5854", requirement = "PEG-5256")
    @Test
    public void setPreferredNonExistingLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String nonExistingLocationId = UUID.randomUUID().toString();

        UserHelper.setPreferredLocation(token, nonExistingLocationId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5853", requirement = "PEG-5256")
    @Test
    public void setPreferredOtherOrganizationLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner()
                .getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationLocationId = locationFlows.createLocation(otherOrganizationId).getString("id");
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        UserHelper.setPreferredLocation(token, otherOrganizationLocationId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5900", requirement = "PEG-5256")
    @Test
    public void setPreferredLocationTwice() {
        final Role randomRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5901", requirement = "PEG-5256")
    @Test
    public void resetPreferredLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String location2id = locationFlows.createInactiveLocation(organizationId).getString("id");
        final String token = userFlows.createUser(organizationId, randomRole, Arrays.asList(location2id, locationId)).getString("token");

        UserHelper.setPreferredLocation(token, locationId)
                .then()
                .statusCode(SC_OK);

        UserHelper.setPreferredLocation(token, location2id)
                .then()
                .statusCode(SC_OK);
    }

}
