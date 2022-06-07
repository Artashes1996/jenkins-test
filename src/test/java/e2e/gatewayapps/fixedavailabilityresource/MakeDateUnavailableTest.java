package e2e.gatewayapps.fixedavailabilityresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fixedavailabilitiesresource.FixedAvailabilitiesHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ResourceFlows;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import static configuration.Role.*;
import static helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class MakeDateUnavailableTest extends BaseTest {

    private LocationFlows locationFlows;
    private ResourceFlows resourceFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;
    private String date;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        resourceFlows = new ResourceFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
    }

    @Xray(test = "PEG-5299", requirement = "PEG-4585")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void makeDayUnavailableBySupportedUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject makeUnavailableBody = new JSONObject();

        makeUnavailableBody.put(DATE, date);
        makeUnavailableBody.put(RESOURCE_ID, resourceId);

        FixedAvailabilitiesHelper.makeDayUnavailable(token, organizationId, locationId, makeUnavailableBody)
                .then()
                .statusCode(SC_OK)
                .body("organizationId", is(organizationId))
                .body("resourceId", is(resourceId))
                .body("locationId", is(locationId))
                .body("date", is(date))
                .body("fromTime", is("00:00"))
                .body("toTime", is("23:59"));

    }

    @Xray(test = "PEG-5339", requirement = "PEG-4585")
    @Test
    public void makeUnavailablePastDate() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject makeUnavailableBody = new JSONObject();
        final String date = DateTimeFormatter.ofPattern("yyy-MM-dd").format(LocalDateTime.now().minusYears(1));
        makeUnavailableBody.put(DATE, date);
        makeUnavailableBody.put(RESOURCE_ID, resourceId);

        FixedAvailabilitiesHelper.makeDayUnavailable(token, organizationId, locationId, makeUnavailableBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Xray(test = "PEG-5341", requirement = "PEG-4585")
    @Test
    public void makeUnavailableOnOtherLocation() {
        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(locationId, otherLocationId)).getString("id");

        final JSONObject makeUnavailableBody = new JSONObject();
        final String date = DateTimeFormatter.ofPattern("yyy-MM-dd").format(LocalDateTime.now().minusYears(1));
        makeUnavailableBody.put(DATE, date);
        makeUnavailableBody.put(RESOURCE_ID, resourceId);

        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");

        FixedAvailabilitiesHelper.makeDayUnavailable(locationAdminToken, organizationId, otherLocationId, makeUnavailableBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

}
