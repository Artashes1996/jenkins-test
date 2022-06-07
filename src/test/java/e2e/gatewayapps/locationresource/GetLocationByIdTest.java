package e2e.gatewayapps.locationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Collections;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class GetLocationByIdTest extends BaseTest {

    private String organizationId;
    private OrganizationFlows organizationFlows;
    private String mainLocationId;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    private UserFlows userFlows;

    private String ownerWithLocationToken;
    private String adminWithLocationToken;
    private String physicalLocationId;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final AuthenticationFlowHelper authenticationFlowHelper = new AuthenticationFlowHelper();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        mainLocationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToken = authenticationFlowHelper.getTokenWithEmail(owner.getString("email"));
        adminToken = authenticationFlowHelper.getTokenWithEmail(admin.getString("email"));
        locationAdminToken = authenticationFlowHelper.getTokenWithEmail(locationAdmin.getString("email"));
        staffToken = authenticationFlowHelper.getTokenWithEmail(staff.getString("email"));

        final JSONObject ownerWithLocation = userFlows.createUser(organizationId, OWNER, Collections.singletonList(mainLocationId));
        final JSONObject adminWithLocation = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(mainLocationId));
        ownerWithLocationToken = authenticationFlowHelper.getTokenWithEmail(ownerWithLocation.getString("email"));
        adminWithLocationToken = authenticationFlowHelper.getTokenWithEmail(adminWithLocation.getString("email"));

        physicalLocationId = new LocationFlows().createLocation(organizationId).getString("id");
    }

    @Xray(test = "PEG-1538")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;

        LocationsHelper.getLocation(token, organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"));
    }

    // TODO add Xray, btw there are documented Xrays but they are not linked
    @Test
    public void getOtherLocationWithRolesHavingLocation() {
        LocationsHelper.getLocation(staffToken, organizationId, physicalLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(locationAdminToken, organizationId, physicalLocationId)
                .then()
                .statusCode(SC_OK);

        LocationsHelper.getLocation(adminWithLocationToken, organizationId, physicalLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(ownerWithLocationToken, organizationId, physicalLocationId)
                .then()
                .statusCode(SC_OK);

        LocationsHelper.getLocation(adminWithLocationToken, organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(ownerWithLocationToken, organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
    }

    // TODO add Xray, btw there are documented Xrays but they are not linked
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void getInactiveLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : role.equals(ADMIN) ? adminToken : role.equals(LOCATION_ADMIN) ? locationAdminToken : staffToken;
        final LocationFlows locationFlows = new LocationFlows();
        locationFlows.inactivateLocation(organizationId, mainLocationId);
        LocationsHelper.getLocation(token, organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
        locationFlows.activateLocation(organizationId, mainLocationId);
    }

    // TODO add Xray, btw there are documented Xrays but they are not linked
    @Test
    public void getLocationOfPausedOrganization() {
        final JSONObject organizationAndUsers =  organizationFlows.createPausedOrganizationWithAllUsers();
        final String organizationId =  organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String mainLocationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(organizationAndUsers.getJSONObject(OWNER.name()).getString("token"), organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token"), organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
    }

    // TODO add Xray
    @Test
    public void getLocationOfBlockedOrganization() {
        final JSONObject organizationAndUsers =  organizationFlows.createBlockedOrganizationWithAllUsers();
        final String organizationId =  organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String mainLocationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(organizationAndUsers.getJSONObject(ADMIN.name()).getString("token"), organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
        LocationsHelper.getLocation(organizationAndUsers.getJSONObject(STAFF.name()).getString("token"), organizationId, mainLocationId)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-1736")
    @Test
    public void getLocationOfDeletedOrganization() {
        final JSONObject organizationAndUsers =  organizationFlows.createBlockedOrganizationWithAllUsers();
        final String organizationId =  organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String mainLocationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        organizationFlows.deleteOrganization(organizationId);

        LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, mainLocationId)
                .then()
                .statusCode(SC_NOT_FOUND);
        LocationsHelper.getLocation(organizationAndUsers.getJSONObject(OWNER.name()).getString("token"), organizationId, mainLocationId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        LocationsHelper.getLocation(organizationAndUsers.getJSONObject(STAFF.name()).getString("token"), organizationId, mainLocationId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    // TODO add Xray, btw there are documented Xrays but they are not linked
    @Test
    public void getLocationWithInactiveUser() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        final String newOrganizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final JSONObject newAdmin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject newLocationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());

        final String newAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(newAdmin.getString("email"));
        final String newLocationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(newLocationAdmin.getString("email"));

        userFlows.inactivateUserById(newOrganizationId, newAdmin.getString("id"));
        userFlows.inactivateUserById(newOrganizationId, newLocationAdmin.getString("id"));

        LocationsHelper.getLocation(newAdminToken, newOrganizationId, newLocationId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
        LocationsHelper.getLocation(newLocationAdminToken, newOrganizationId, newLocationId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
