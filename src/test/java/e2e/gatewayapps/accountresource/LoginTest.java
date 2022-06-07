package e2e.gatewayapps.accountresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.accountresource.LoginHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.util.Collections;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static utils.TestUtils.*;

public class LoginTest extends BaseTest {

    private String organizationId;
    private JSONObject organizationAndUsers;
    private String password;
    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        password = "Qw!123456";
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    // TODO XRay is missing
    @Test
    public void checkSupportLogin() {
        LoginHelper.login(SUPPORT)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
    }

    // TODO XRay is missing
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void checkLogin(Role role) {
        final String email = organizationAndUsers.getJSONObject(role.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
    }

    // TODO XRay is missing
    @Test
    public void checkLoginPausedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        String email = pausedOrganization.getJSONObject(OWNER.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
        email = pausedOrganization.getJSONObject(ADMIN.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
        email = pausedOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
        email = pausedOrganization.getJSONObject(STAFF.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
    }

    // TODO XRay is missing
    @Test
    public void checkLoginBlockedOrganization() {
        final JSONObject blockedOrganization = organizationFlows.createBlockedOrganizationWithAllUsers();
        String email = blockedOrganization.getJSONObject(OWNER.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
        email = blockedOrganization.getJSONObject(ADMIN.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
        email = blockedOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
        email = blockedOrganization.getJSONObject(STAFF.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/login.json"));
    }

    // TODO XRay is missing
    @Test
    public void checkLoginDeletedOrganization() {
        final JSONObject deletedOrganization = organizationFlows.createAndDeletePublishedOrganization();
        String email = deletedOrganization.getJSONObject(OWNER.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        email = deletedOrganization.getJSONObject(ADMIN.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        email = deletedOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        email = deletedOrganization.getJSONObject(STAFF.name()).getString("email");
        LoginHelper.login(email, password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // TODO XRay is missing
    @SneakyThrows
    @Test
    public void checkLoginInactiveUser() {
        final JSONObject inactiveOwner = userFlows.createUser(organizationId, OWNER, null);
        userFlows.inactivateUserById(organizationId, inactiveOwner.getString("id"));
        Thread.sleep(500);
        LoginHelper.login(inactiveOwner.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        final JSONObject inactiveAdmin = userFlows.createUser(organizationId, ADMIN, null);
        userFlows.inactivateUserById(organizationId, inactiveAdmin.getString("id"));
        Thread.sleep(500);
        LoginHelper.login(inactiveAdmin.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject inactiveLocationAdmin = userFlows.createUser(organizationId, LOCATION_ADMIN, Collections.singletonList(locationId));
        userFlows.inactivateUserById(organizationId, inactiveLocationAdmin.getString("id"));
        Thread.sleep(500);
        LoginHelper.login(inactiveLocationAdmin.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        final JSONObject inactiveStaff = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId));
        userFlows.inactivateUserById(organizationId, inactiveStaff.getString("id"));
        Thread.sleep(500);
        LoginHelper.login(inactiveStaff.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // TODO XRay is missing
    @Test
    public void checkLoginDeletedUser() {
        final JSONObject deletedOwner = userFlows.createUser(organizationId, OWNER, null);
        userFlows.deleteUser(organizationId, deletedOwner.getString("id"));
        LoginHelper.login(deletedOwner.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        final JSONObject deletedAdmin = userFlows.createUser(organizationId, ADMIN, null);
        userFlows.deleteUser(organizationId, deletedAdmin.getString("id"));
        LoginHelper.login(deletedAdmin.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final JSONObject deletedLocationAdmin = userFlows.createUser(organizationId, LOCATION_ADMIN, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, deletedLocationAdmin.getString("id"));
        LoginHelper.login(deletedLocationAdmin.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        final JSONObject deletedStaff = userFlows.createUser(organizationId, STAFF, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, deletedStaff.getString("id"));
        LoginHelper.login(deletedStaff.getString("email"), password)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // TODO XRay is missing
    @Test
    public void checkLoginWithWrongCredentials() {
        final JSONObject body = new JSONObject();
        body.put("email", getRandomInt() + "@qa.notvalid");
        body.put("password", "Qw123456&");
        LoginHelper.login(body)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

}
