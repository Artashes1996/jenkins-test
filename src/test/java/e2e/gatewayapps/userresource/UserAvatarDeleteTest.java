package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.gatewayapps.userresource.data.UserDataProvider;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.util.*;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class UserAvatarDeleteTest extends BaseTest {

    private String organizationId;
    private String locationId;

    private UserFlows userFlows;
    private OrganizationFlows organizationFlows;
    private JSONObject orgWithAllUsers;


    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        orgWithAllUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = orgWithAllUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = orgWithAllUsers.getJSONObject("LOCATION").getString("id");
    }

    @Test(testName = "PEG-1456, PEG-1457", dataProviderClass = UserDataProvider.class, dataProvider = "valid avatar paths")
    public void deleteAvatar(String filePath) {
        userFlows.uploadAvatar(SUPPORT_TOKEN);
        UserHelper.deleteUserAvatar(SUPPORT_TOKEN)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-1455", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteAvatarAllRoles(Role role) {
        final String token = orgWithAllUsers.getJSONObject(role.name()).getString("token");
        userFlows.uploadAvatar(token);
        UserHelper.deleteUserAvatar(token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test(testName = "PEG-1458")
    public void deleteAvatarInvalidToken() {
        final String invalidToken = UUID.randomUUID().toString();

        UserHelper.deleteUserAvatar(invalidToken)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test(testName = "PEG-1459, PEG-1460", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteAvatarInactiveUser(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String userToken = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));

        userFlows.uploadAvatar(userToken);
        userFlows.inactivateUserById(organizationId, user.getString("id"));

        UserHelper.deleteUserAvatar(userToken)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test(testName = "PEG-1483, PEG-1484", dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteAvatarDeletedUser(Role role) {
        final JSONObject user = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        final String userToken = new AuthenticationFlowHelper().getTokenWithEmail(user.getString("email"));

        userFlows.uploadAvatar(userToken);
        userFlows.deleteUser(organizationId, user.getString("id"));

        UserHelper.deleteUserAvatar(userToken)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Test
    public void deleteAvatarPausedOrganization() {
        final JSONObject orgAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = orgAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final Role role = getRandomOrganizationRole();
        final JSONObject newUser = new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = newUser.getString("token");
        organizationFlows.pauseOrganization(organizationId);
        userFlows.uploadAvatar(token);
        UserHelper.deleteUserAvatar(token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void deleteAvatarBlockedOrganization() {
        final JSONObject orgAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = orgAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final Role role = getRandomOrganizationRole();
        final JSONObject newUser = new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = newUser.getString("token");
        organizationFlows.blockOrganization(organizationId);
        userFlows.uploadAvatar(token);
        UserHelper.deleteUserAvatar(token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void deleteAvatarDeletedOrganization() {
        final JSONObject orgAndOwner = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = orgAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        final Role role = getRandomOrganizationRole();
        final JSONObject newUser = new UserFlows().createUser(organizationId, role, Collections.singletonList(locationId));
        final String token = newUser.getString("token");
        organizationFlows.deleteOrganization(organizationId);

        UserHelper.deleteUserAvatar(token)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

}
