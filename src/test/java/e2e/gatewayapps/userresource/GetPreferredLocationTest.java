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

import static org.hamcrest.Matchers.*;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetPreferredLocationTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;
    private JSONObject location;
    
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        location = organizationAndUsers.getJSONObject("LOCATION");
        locationId = location.getString("id");
    }

    @Xray(test = "PEG-5881", requirement = "PEG-5256")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getPreferredLocationBySupportedUsers(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        userFlows.setPreferredLocation(token, locationId);

        UserHelper.getUserPreferredBasicLocation(token)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/basicLocation.json"))
                .body("id", is(locationId))
                .body("internalName", is(location.getString("internalName")));
    }

    @Xray(test = "PEG-5882", requirement = "PEG-5256")
    @Test
    public void getPreferredLocationUnSupportedUser() {
        UserHelper.getUserPreferredBasicLocation(SUPPORT_TOKEN)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

    @Xray(test = "PEG-5883", requirement = "PEG-5256")
    @Test
    public void notSetAndGetPreferredLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final String userToken = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId)).getString("token");

        UserHelper.getUserPreferredBasicLocation(userToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5884", requirement = "PEG-5256")
    @Test
    public void getPreferredLocationBlockedOrganization() {
        final Role role = getRandomOrganizationRole();
        final JSONObject blockedOrganization = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String token = blockedOrganization.getJSONObject(role.name()).getString("token");
        final JSONObject blockedOrganizationLocation = blockedOrganization.getJSONObject("LOCATION");
        userFlows.setPreferredLocation(token, blockedOrganizationLocation.getString("id"));

        UserHelper.getUserPreferredBasicLocation(token)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/basicLocation.json"))
                .body("id", is(blockedOrganizationLocation.getString("id")))
                .body("internalName", is(blockedOrganizationLocation.getString("internalName")));
    }

    @Xray(test = "PEG-5885", requirement = "PEG-5256")
    @Test
    public void getPreferredLocationPausedOrganization() {
        final Role role = getRandomOrganizationRole();
        final JSONObject pausedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        final String token = pausedOrganization.getJSONObject(role.name()).getString("token");
        final JSONObject pausedOrganizationLocation = pausedOrganization.getJSONObject("LOCATION");
        userFlows.setPreferredLocation(token, pausedOrganizationLocation.getString("id"));

        UserHelper.getUserPreferredBasicLocation(token)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/basicLocation.json"))
                .body("id", is(pausedOrganizationLocation.getString("id")))
                .body("internalName", is(pausedOrganizationLocation.getString("internalName")));
    }

    @Xray(test = "PEG-5886", requirement = "PEG-5256")
    @Test
    public void getPreferredInactiveLocation() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject inactiveLocation = locationFlows.createLocation(organizationId);
        final String inactiveLocationId = inactiveLocation.getString("id");
        final String token = userFlows.createUser(organizationId, randomRole, Collections.singletonList(inactiveLocationId))
                .getString("token");
        userFlows.setPreferredLocation(token, inactiveLocationId);
        locationFlows.inactivateLocation(organizationId, inactiveLocationId);

        UserHelper.getUserPreferredBasicLocation(token)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/basicLocation.json"))
                .body("id", is(inactiveLocationId))
                .body("internalName", is(inactiveLocation.getString("internalName")))
                .body("status", is("INACTIVE"));
    }

    @Xray(test = "PEG-5899", requirement = "PEG-5256")
    @Test
    public void getByNonExistingToken() {
        final String invalidToken = UUID.randomUUID().toString();
        UserHelper.getUserPreferredBasicLocation(invalidToken)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-5902", requirement = "PEG-5256")
    @Test
    public void resetPreferredLocationAndGetIt() {
        final Role randomRole = getRandomOrganizationRole();
        final JSONObject location2 = locationFlows.createLocation(organizationId);
        final String location2Id = location2.getString("id");
        final String token = userFlows.createUser(organizationId, randomRole, Arrays.asList(locationId, location2Id)).getString("token");
        userFlows.setPreferredLocation(token, locationId);
        userFlows.setPreferredLocation(token, location2Id);

        UserHelper.getUserPreferredBasicLocation(token)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/basicLocation.json"))
                .body("id", is(location2Id))
                .body("internalName", is(location2.getString("internalName")));
    }

}
