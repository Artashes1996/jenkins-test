package helpers.flows;

import helpers.appsapi.locationsresource.payloads.LocationStatusChangeRequestBody;
import helpers.appsapi.locationsresource.payloads.LocationUpdateRequestBody;
import helpers.appsapi.locationsresource.payloads.ToggleLinkServicesToLocationRequestBody;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.ToggleLinkLocationsToServiceRequestBody;
import helpers.appsapi.support.locationresource.LocationsHelper;
import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import helpers.appsapi.support.locationresource.payloads.TimeZones;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.commons.ToggleAction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;
import static org.apache.http.HttpStatus.SC_OK;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class LocationFlows {

    public final CreateLocationRequestBody createLocationRequestBody = new CreateLocationRequestBody();
    public final LocationUpdateRequestBody locationUpdateRequestBody = new LocationUpdateRequestBody();

    public JSONObject createLocation(String organizationId) {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        return new JSONObject(LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .extract().asString()
        );
    }

    public JSONObject createInactiveLocation(String organizationId) {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.put(STATUS, LocationStatuses.INACTIVE);
        return new JSONObject(LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .extract().asString()
        );
    }


    public JSONArray createLocations(String organizationId, int count) {
        final JSONArray locations = new JSONArray();
        IntStream.range(0, count).mapToObj(j -> createLocation(organizationId)).forEach(locations::put);
        return locations;
    }

    public ArrayList<String> createLocationsAndReturnIdsOnly(String organizationId, int count) {
        final ArrayList<String> locations = new ArrayList<>();
        IntStream.range(0, count).mapToObj(j -> createLocation(organizationId).getString("id")).forEach(locations::add);

        return locations;
    }

    public void inactivateLocation(String organizationId, String locationId) {
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationsAndStatuses = new JSONObject();
        locationsAndStatuses.put(locationId, LocationStatuses.INACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        helpers.appsapi.locationsresource.LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    public void activateLocation(String organizationId, String locationId) {
        final JSONObject statusChangeRequestBody = new JSONObject();
        final JSONObject locationsAndStatuses = new JSONObject();
        locationsAndStatuses.put(locationId, LocationStatuses.ACTIVE.name());
        statusChangeRequestBody.put(LocationStatusChangeRequestBody.STATUSES, locationsAndStatuses);
        helpers.appsapi.locationsresource.LocationsHelper.changeLocationsStatuses(SUPPORT_TOKEN, organizationId, statusChangeRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    public JSONArray createActiveInactiveLocations(String organizationId, int activeCount, int inactiveCount) {
        final JSONArray locations = new JSONArray();
        IntStream.range(0, inactiveCount).mapToObj(i -> createInactiveLocation(organizationId)).forEach(locations::put);
        IntStream.range(0, activeCount).mapToObj(i -> createLocation(organizationId)).forEach(locations::put);
        return locations;
    }

    public void linkUnlinkLocationsToService(String organizationId, String serviceId, String locationId, ToggleAction action) {
        final JSONObject toggleLinkBody = ToggleLinkLocationsToServiceRequestBody.bodyBuilder(action, new JSONArray().put(locationId));
        ServicesHelper.linkUnlinkLocationsToService(SUPPORT_TOKEN, organizationId, serviceId, toggleLinkBody)
                .then().statusCode(SC_OK);
    }

    public void linkUnlinkServicesToLocation(String organizationId, String locationId, List<String> serviceIds, ToggleAction toggleAction) {
        final JSONArray servicesArray = new JSONArray();
        for (String serviceId : serviceIds) {
            servicesArray.put(serviceId);
        }
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(toggleAction,
                servicesArray);
        helpers.appsapi.locationsresource.LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then().statusCode(SC_OK);
    }

    public JSONObject createLocationInTimezone(String organizationId, String timeZone) {
        final JSONObject createLocationBody = createLocationRequestBody.bodyBuilder(CreateLocationCombination.ALL_FIELDS);
        createLocationBody.put(TIMEZONE, timeZone);
        return new JSONObject(LocationsHelper.createLocation(SUPPORT_TOKEN, createLocationBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .extract().asString());
    }

    public JSONObject createLocationByUtcOffset(String organizationId, int offset) {
        return createLocationInTimezone(organizationId, TimeZones.getTimeZoneByUtcOffset(offset));
    }

    public JSONObject updateLocation(String organizationId, String locationId) {
        final JSONObject locationUpdateBody = locationUpdateRequestBody.bodyBuilder(LocationUpdateRequestBody.EditLocationCombination.ALL_FIELDS);
        return new JSONObject(helpers.appsapi.locationsresource.LocationsHelper.updateLocation(SUPPORT_TOKEN, organizationId, locationId, locationUpdateBody)
                .then()
                .statusCode(SC_OK)
                .extract().asString());

    }

    public JSONObject createLocation(String organizationId, JSONObject locationCreationRequestBody) {
        return new JSONObject(LocationsHelper.createLocation(SUPPORT_TOKEN, locationCreationRequestBody, organizationId)
                .then()
                .statusCode(SC_CREATED)
                .extract().asString());
    }

}
