package helpers.flows;

import helpers.appsapi.recurringavailabilitiesresource.RecurringAvailabilitiesHelper;
import helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.util.Arrays;

import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class RecurringAvailabilitiesFlows {

    public JSONObject createRecurringAvailability(String organizationId, String locationId, String resourceId, DayOfWeek dayOfWeek, String from, String to) {
        final JSONArray availabilitySlot = createRecurringAvailability(organizationId, locationId, resourceId, dayOfWeek, timeslotsGenerator(from, to));
        return availabilitySlot.getJSONObject(0);
    }

    public JSONArray createRecurringAvailability(String organizationId, String locationId, String resourceId, DayOfWeek dayOfTheWeek, JSONArray timeSlots) {
        final JSONObject recurringBody = new JSONObject();
        recurringBody.put(RESOURCE_ID, resourceId);
        recurringBody.put(DAY_OFF_WEEK, dayOfTheWeek.name());
        recurringBody.put(TIME_SLOTS, timeSlots);
        return new JSONArray(RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, organizationId, locationId, recurringBody)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }

    public JSONArray createRecurringAvailability(String organizationId, String locationId, String resourceId) {
        final JSONObject recurringBody = new CreateRecurringRequestBody().bodyBuilder(resourceId);
        return new JSONArray(RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, organizationId, locationId, recurringBody)
                .then()
                .statusCode(SC_OK)
                .extract().body().asString());
    }

    public JSONArray createRecurringAvailabilityForWeek(String organizationId, String locationId, String resourceId) {
        final JSONObject recurringBody = new JSONObject();
        recurringBody.put(RESOURCE_ID, resourceId);
        recurringBody.put(TIME_SLOTS, timeslotsGenerator("09:00", "19:00"));
        final JSONArray recurringAvailabilities = new JSONArray();

        Arrays.stream(DayOfWeek.values()).forEach(day -> {
            recurringBody.put(DAY_OFF_WEEK, day.name());
            final JSONArray response = new JSONArray(RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, organizationId, locationId, recurringBody)
                    .then().statusCode(SC_OK).extract().body().asString());
            recurringAvailabilities.put(response.get(0));
        });
        return recurringAvailabilities;
    }

    public JSONArray createHourlyTimeSlots(int count) {
        if (count > 22) {
            throw new IllegalArgumentException("Maximum hourly count should be 22");
        }
        final JSONArray timeSlots = new JSONArray();
        for (int i = 0; i < count; i++) {
            final JSONObject timeSlot = new JSONObject();
            timeSlot.put(FROM, "0" + i + ":01");
            timeSlot.put(TO, "0" + (i + 1) + ":00");
            timeSlots.put(timeSlot);
        }
        return timeSlots;
    }

    private JSONArray timeslotsGenerator(String from, String to) {
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, from);
        timeSlot.put(TO, to);
        timeSlots.put(timeSlot);
        return timeSlots;
    }

}
