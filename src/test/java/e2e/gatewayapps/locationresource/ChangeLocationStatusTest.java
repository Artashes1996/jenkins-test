package e2e.gatewayapps.locationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.locationresource.data.LocationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.locationsresource.payloads.LocationStatusChangeRequestBody;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import helpers.flows.LocationFlows;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;


import java.util.*;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;


public class ChangeLocationStatusTest extends BaseTest {

    private List<String> locationsIds;
    private Map<String, JSONObject> locationsParams;
    private String organizationId;
    private OrganizationFlows organizationFlows;
    private String mainLocationId;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        mainLocationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        ownerToken = new AuthenticationFlowHelper().getTokenWithEmail(owner.getString("email"));
        adminToken = new AuthenticationFlowHelper().getTokenWithEmail(admin.getString("email"));
        locationAdminToken = new AuthenticationFlowHelper().getTokenWithEmail(locationAdmin.getString("email"));
        staffToken = new AuthenticationFlowHelper().getTokenWithEmail(staff.getString("email"));

        final int locationCount = 10;
        final LocationFlows locationFlows = new LocationFlows();

        for (int i = 0; i < locationCount; i++) {
            final JSONObject physicalLocation = locationFlows.createLocation(organizationId);
            final JSONObject virtualLocation = locationFlows.createLocation(organizationId);
            final JSONObject physicalInactiveLocation = locationFlows.createInactiveLocation(organizationId);
            final JSONObject virtualInactiveLocation = locationFlows.createInactiveLocation(organizationId);

            final String physicalLocationId = physicalLocation.getString("id");
            final String virtualLocationId = virtualLocation.getString("id");
            final String physicalInactiveLocationId = physicalInactiveLocation.getString("id");
            final String virtualInactiveLocationId = virtualInactiveLocation.getString("id");

            locationsParams = new HashMap<>();
            locationsParams.put(physicalLocationId, physicalLocation);
            locationsParams.put(virtualLocationId, virtualLocation);
            locationsParams.put(physicalInactiveLocationId, physicalInactiveLocation);
            locationsParams.put(virtualInactiveLocationId, virtualInactiveLocation);

            locationsIds = new ArrayList<>();
            locationsIds.add(physicalLocationId);
            locationsIds.add(virtualLocationId);
            locationsIds.add(physicalInactiveLocationId);
            locationsIds.add(virtualInactiveLocationId);
        }
    }

    @Test(testName = "PEG-1519, PEG-1520, PEG-1521, PEG-1522", dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void inactivateActivateLocations(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationsAndStatuses = new JSONObject();
        locationsIds.forEach(location -> locationsAndStatuses.put(location, LocationStatuses.ACTIVE.name()));
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        LocationsHelper.changeLocationsStatuses(token, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);

        locationsIds.forEach(location -> LocationsHelper.getLocation(token, organizationId, location)
                .then()
                .assertThat().body("status", equalTo(LocationStatuses.ACTIVE.name())));

        locationsIds.forEach(location -> locationsAndStatuses.put(location, LocationStatuses.INACTIVE.name()));
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        LocationsHelper.changeLocationsStatuses(token, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);

        locationsIds.forEach(location -> LocationsHelper.getLocation(token, organizationId, location)
                .then()
                .assertThat().body("status", equalTo(LocationStatuses.INACTIVE.name())));
    }

    @Test
    public void inactivateActivateLocationsStaff() {
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationsAndStatuses = new JSONObject();
        locationsAndStatuses.put(mainLocationId, LocationStatuses.ACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        LocationsHelper.changeLocationsStatuses(staffToken, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void inactivateActivateLocationByLocationAdmin() {
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationsAndStatuses = new JSONObject();
        locationsAndStatuses.put(mainLocationId, LocationStatuses.ACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        LocationsHelper.changeLocationsStatuses(locationAdminToken, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void inactivateActivateOtherLocationByLocationAdmin() {
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationsAndStatuses = new JSONObject();
        locationsAndStatuses.put(locationsIds.get(0), LocationStatuses.ACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        LocationsHelper.changeLocationsStatuses(locationAdminToken, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-1525, PEG-1526", priority = 10)
    public void changeStatusOfPausedStateOrganizationLocations() {
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        organizationFlows.pauseOrganization(organizationId);

        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject statuses = new JSONObject();
        statuses.put(locationId, LocationStatuses.INACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, statuses);

        LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-1527, PEG-1528", priority = 20)
    public void changeStatusOfBlockedStateOrganizationLocations() {
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        organizationFlows.blockOrganization(organizationId);

        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject statuses = new JSONObject();
        statuses.put(locationId, LocationStatuses.INACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, statuses);

        LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-1524")
    public void changeStatusOfDeletedOrganizationLocation() {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = new LocationFlows().createLocation(organizationId).getString("id");
        organizationFlows.deleteOrganization(organizationId);
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject statuses = new JSONObject();
        statuses.put(locationId, LocationStatuses.INACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, statuses);

        LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test(testName = "PEG-1529", dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void changeStatusOfNonExistingLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : role.equals(OWNER) ? ownerToken : adminToken;
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject statuses = new JSONObject();
        final String locationId = locationsIds.get(new Random().nextInt(locationsIds.size()));
        final String existingLocationCurrentStatus = LocationsHelper.getLocation(token, organizationId, locationId).then().extract().path("status");
        final String changeStatus = existingLocationCurrentStatus.equals(LocationStatuses.ACTIVE.name()) ? LocationStatuses.INACTIVE.name() : LocationStatuses.ACTIVE.name();

        statuses.put(locationId, changeStatus);
        statuses.put(UUID.randomUUID().toString(), LocationStatuses.ACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, statuses);

        LocationsHelper.changeLocationsStatuses(token, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_NOT_FOUND);
        LocationsHelper.getLocation(token, organizationId, locationId)
                .then()
                .assertThat()
                .body("status", equalTo(locationsParams.get(locationId).getString("status")));
    }

    @Test(testName = "PEG-1530")
    public void changeLocationStatusIncludingOtherOrganizationLocation() {
        final JSONObject unpublishedOrganizationWithOwner = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String otherOrganizationId = unpublishedOrganizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationLocationId = new LocationFlows().createLocation(otherOrganizationId).getString("id");
        final String locationId = locationsIds.get(new Random().nextInt(locationsIds.size()));
        final JSONObject statusChangeBody = new JSONObject();
        final JSONObject statuses = new JSONObject();

        final String existingLocationCurrentStatus = LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, locationId).then().extract().path("status");
        final String changeStatus = existingLocationCurrentStatus.equals(LocationStatuses.ACTIVE.name()) ? LocationStatuses.INACTIVE.name() : LocationStatuses.ACTIVE.name();

        statuses.put(locationId, changeStatus);
        statuses.put(otherOrganizationLocationId, LocationStatuses.INACTIVE.name());
        statusChangeBody.put(LocationStatusChangeRequestBody.STATUSES, statuses);

        LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeBody)
                .then()
                .statusCode(SC_NOT_FOUND);

        LocationsHelper.getLocation(SUPPORT_TOKEN, organizationId, locationId)
                .then()
                .assertThat()
                .body("status", equalTo(locationsParams.get(locationId).getString("status")));
    }

    @Test(testName = "PEG-1531", dataProvider = "location invalid status", dataProviderClass = LocationDataProvider.class)
    public void changeStatusToInvalid(Object invalidStatuses) {
        final JSONObject statusChangeBody = new JSONObject();
        final JSONObject statuses = new JSONObject();

        statuses.put(locationsIds.get(new Random().nextInt(locationsIds.size())), invalidStatuses);
        statusChangeBody.put(LocationStatusChangeRequestBody.STATUSES, statuses);

        LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    //TODO complete when location deletion will be ready
    @Test(testName = "PEG-1532", enabled = false)
    public void changeStatusOfDeletedLocation() {
        final String organizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getString("id");
        final LocationFlows locationFlows = new LocationFlows();
        final String locationId = locationFlows.createLocation(organizationId).getString("id");

    }

}
