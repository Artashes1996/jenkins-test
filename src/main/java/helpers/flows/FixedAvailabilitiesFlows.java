package helpers.flows;

import helpers.appsapi.fixedavailabilitiesresource.FixedAvailabilitiesHelper;
import helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.apache.http.HttpStatus.*;
import static helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class FixedAvailabilitiesFlows {
    private final FixedAvailabilityUpsertBody fixedAvailabilityUpsertBody = new FixedAvailabilityUpsertBody();

    public JSONObject createFixedAvailability(String organizationId, String locationId, String resourceId, String date, String from, String to) {
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        fixedAvailabilityCreateBody.put(DATE, date);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, from);
        timeSlot.put(TO, to);
        timeSlots.put(timeSlot);
        fixedAvailabilityCreateBody.put(TIME_SLOTS, timeSlots);
        return new JSONArray(FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK).extract().body().asString()).getJSONObject(0);
    }

    public JSONObject createFixedAvailabilityAllDay(String organizationId, String locationId, String resourceId, String date) {
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        fixedAvailabilityCreateBody.put(DATE, date);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "00:00");
        timeSlot.put(TO, "23:59");
        timeSlots.put(timeSlot);
        fixedAvailabilityCreateBody.put(TIME_SLOTS, timeSlots);

        return new JSONArray(FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, fixedAvailabilityCreateBody)
                .then().statusCode(SC_OK).extract().body().asString()).getJSONObject(0);

    }

    public JSONObject makeDayUnavailable(String organizationId, String locationId, String resourceId, String date) {
        final JSONObject makeUnavailableBody = new JSONObject();

        makeUnavailableBody.put(DATE, date);
        makeUnavailableBody.put(RESOURCE_ID, resourceId);

        return new JSONObject(FixedAvailabilitiesHelper.makeDayUnavailable(SUPPORT_TOKEN, organizationId, locationId, makeUnavailableBody)
                .then()
                .statusCode(SC_OK).extract().body().asString());
    }

    public JSONObject createFixedAvailability(String organizationId, String locationId, JSONObject body) {
        return new JSONArray(FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, body)
                .then()
                .statusCode(SC_OK).extract().body().asString()).getJSONObject(0);
    }

    public void eraseFixedAvailability(String organizationId, String locationId, String resourceId, String date) {
        final JSONObject body = new JSONObject();
        final JSONArray timeSlots = new JSONArray();
        body.put(DATE, date);
        body.put(RESOURCE_ID, resourceId);
        body.put(TIME_SLOTS, timeSlots);
        FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, body)
                .then().statusCode(SC_OK);
    }

}
