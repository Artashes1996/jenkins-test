package e2e.gatewayapps.locationresource;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.locationsresource.payloads.LocationUpdateRequestBody;
import helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;
import utils.commons.ToggleAction;

import java.util.Arrays;
import java.util.Collections;

import static helpers.appsapi.locationsresource.payloads.LocationUpdateRequestBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static configuration.Role.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

//TODO there aren't any xrays

public class UpdateLocationTest {

    private LocationUpdateRequestBody locationUpdateRequestBody;
    private String organizationId;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private JSONObject location;
    private String mainLocationId;
    private JSONObject organizationAndUsers;

    @BeforeClass
    public void setUp() {
        locationUpdateRequestBody = new LocationUpdateRequestBody();
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        location = organizationAndUsers.getJSONObject("LOCATION");
        mainLocationId = location.getString("id");
    }

    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void changeLocationFields(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);

        LocationsHelper.updateLocation(token, organizationId, mainLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"))
                .body("internalName", is(updateLocationBody.getString("internalName")))
                .body("nameTranslation", is(updateLocationBody.getString("nameTranslation")))
                .body("status", is(updateLocationBody.getString("status")))
                .body("description", is(updateLocationBody.getString("description")))
                .body("phoneNumber", is(updateLocationBody.getString("phoneNumber")))
                .body("address.zipcode", is(updateLocationBody.getJSONObject("address").getString("zipcode")))
                .body("address.country", is(updateLocationBody.getJSONObject("address").getString("country")))
                .body("address.city", is(updateLocationBody.getJSONObject("address").getString("city")))
                .body("address.latitude", is(updateLocationBody.getJSONObject("address").getFloat("latitude")))
                .body("address.addressLine1", is(updateLocationBody.getJSONObject("address").getString("addressLine1")))
                .body("address.stateRegion", is(updateLocationBody.getJSONObject("address").getString("stateRegion")))
                .body("address.addressLine2", is(updateLocationBody.getJSONObject("address").getString("addressLine2")))
                .body("address.longitude", is(updateLocationBody.getJSONObject("address").getFloat("longitude")));
    }

    @Test
    public void changeLocationRequiredFields() {
        final Role role = getRandomInviterRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.REQUIRED_FIELDS);

        LocationsHelper.updateLocation(token, organizationId, mainLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"));
    }

    @Test
    public void changeLocationFieldsByStaff() {
        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        LocationsHelper.updateLocation(staffToken, organizationId, mainLocationId, updateLocationBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void inactivateLocation(Role role) {
        final String activeLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject updateLocationBody = locationUpdateRequestBody
                .bodyBuilder(EditLocationCombination.ALL_FIELDS)
                .put(STATUS, LocationStatuses.INACTIVE.name());

        LocationsHelper.updateLocation(token, organizationId, activeLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"))
                .body("status", is(LocationStatuses.INACTIVE.name()));
    }

    @Test
    public void changeLocationStatusByStaff() {
        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);
        updateLocationBody.put(STATUS, LocationStatuses.INACTIVE.name());

        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");

        LocationsHelper.updateLocation(staffToken, organizationId, mainLocationId, updateLocationBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void changeLocationFieldsPausedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        final String pausedOrganizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);

        final String pausedOrganizationLocationId = pausedOrganization.getJSONObject("LOCATION").getString("id");

        LocationsHelper.updateLocation(SUPPORT_TOKEN, pausedOrganizationId, pausedOrganizationLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK);

        final String adminToken = pausedOrganization.getJSONObject(ADMIN.name()).getString("token");

        LocationsHelper.updateLocation(adminToken, pausedOrganizationId, pausedOrganizationLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void changeLocationFieldsBlockedOrganization() {
        final JSONObject blockedOrganization = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String blockedOrganizationId = blockedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);

        final String blockedOrganizationLocationId = blockedOrganization.getJSONObject("LOCATION").getString("id");

        LocationsHelper.updateLocation(SUPPORT_TOKEN, blockedOrganizationId, blockedOrganizationLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK);

        final String adminToken = blockedOrganization.getJSONObject(ADMIN.name()).getString("token");

        LocationsHelper.updateLocation(adminToken, blockedOrganizationId, blockedOrganizationLocationId, updateLocationBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void changeLocationFieldsDeletedOrganization() {
        final JSONObject deletedOrganization = organizationFlows.createAndDeletePublishedOrganization();
        final String deletedOrganizationId = deletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = deletedOrganization.getJSONObject("LOCATION").getString("id");

        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);

        LocationsHelper.updateLocation(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationLocationId, updateLocationBody)
                .then()
                .statusCode(SC_FORBIDDEN);

        final String ownerToken = deletedOrganization.getJSONObject(OWNER.name()).getString("token");

        LocationsHelper.updateLocation(ownerToken, deletedOrganizationId, deletedOrganizationLocationId, updateLocationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-5796", requirement = "PEG-5221")
    @Test
    public void tryToChangeLocationTimeZone() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject location = locationFlows.createLocation(organizationId);
        final JSONObject updateLocationBody = locationUpdateRequestBody.bodyBuilder(EditLocationCombination.ALL_FIELDS);
        updateLocationBody.put("timezone", "Asia/Qatar");

        LocationsHelper.updateLocation(token, organizationId, location.getString("id"), updateLocationBody)
                .then()
                .statusCode(SC_OK)
                .body("nameTranslation", is(updateLocationBody.getString("nameTranslation")))
                .body("timezone", is(location.getString("timezone")));
    }

}
