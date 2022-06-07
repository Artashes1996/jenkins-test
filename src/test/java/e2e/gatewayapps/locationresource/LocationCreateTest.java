package e2e.gatewayapps.locationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.locationresource.data.LocationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import helpers.appsapi.support.locationresource.LocationsHelper;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.Random;

import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.LocationStatuses.INACTIVE;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class LocationCreateTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private CreateLocationRequestBody createLocationRequestBody;
    private JSONObject organizationAndUsers;
    private String organizationId;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        createLocationRequestBody = new CreateLocationRequestBody();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
    }


    @Xray(test = "PEG-1247")
    @Test
    public void createLocationWithAllFields() {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"));
    }

    @Xray(test = "PEG-1738")
    @Test
    public void createLocationForDeletedOrganization() {
        final String organizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final JSONObject createLocationBody = new CreateLocationRequestBody().bodyBuilder(CreateLocationCombination.ALL_FIELDS);

        organizationFlows.deleteOrganization(organizationId);
        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-1248")
    @Test
    public void createInactiveLocationSupport() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.put(STATUS, INACTIVE);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"))
        ;
    }

    @Xray(test = "PEG-1251")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void createLocationWithOrganizationLevelRoles(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);

        LocationsHelper.createLocation(token, createLocationBody, organizationId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-1252")
    @Test
    public void createTwoLocationsWithSameNameSupport() {
        final String locationName = "Location " + new Random().nextInt();
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.put(INTERNAL_NAME, locationName);

        final JSONObject createLocationBody1 = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody1.put(INTERNAL_NAME, locationName);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Xray(test = "PEG-1256")
    @Test
    public void createTwoLocationsWithSameNameDifferentOrganizationsSupport() {
        final String newOrganizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, newOrganizationId)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"));

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"));
    }

    @Xray(test = "PEG-1253")
    @Test
    public void createLocationMissName() {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.remove(INTERNAL_NAME);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1290")
    @Test
    public void createLocationMissStatus() {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.remove(STATUS);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1289")
    @Test
    public void createLocationMissTimezone() {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.remove(TIMEZONE);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1291")
    @Test
    public void createLocationMissAddress() {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.remove(ADDRESS);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1292")
    @Test(dataProvider = "address required fields", dataProviderClass = LocationDataProvider.class)
    public void createLocationMissFieldsFromAddress(String addressField) {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.getJSONObject(ADDRESS).remove(addressField);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1249")
    @Test
    public void createLocationMissNonRequired() {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.REQUIRED_FIELDS);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createLocation.json"))
                .body("address.latitude", nullValue())
                .body("address.longitude", nullValue())
                .body("phoneNumber", nullValue())
                .body("description", nullValue());
    }

    @Xray(test = "PEG-1255")
    @Test
    public void createLocationIncorrectStatus() {
        final String incorrectStatus = "INCORRECT";
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.REQUIRED_FIELDS);
        createLocationBody.put(STATUS, incorrectStatus);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-1297")
    @Test(dataProvider = "invalid Timezone Values", dataProviderClass = LocationDataProvider.class)
    public void createLocationIncorrectTimezone(Object timezoneIncorrect) {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.put(TIMEZONE, timezoneIncorrect);

        LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}