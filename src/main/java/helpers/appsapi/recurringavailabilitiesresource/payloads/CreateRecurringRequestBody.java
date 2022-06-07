package helpers.appsapi.recurringavailabilitiesresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;

import static helpers.appsapi.recurringavailabilitiesresource.payloads.Days.*;

public class CreateRecurringRequestBody {

    public final static String DAY_OFF_WEEK = "dayOfWeek";
    public final static String RESOURCE_ID = "resourceId";
    public final static String TIME_SLOTS = "timeSlots";
    public final static String FROM = "from";
    public final static String TO = "to";

    public JSONObject bodyBuilder(String resourceId) {
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(DAY_OFF_WEEK, getRandomDayName());
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        timeSlotsArray.put(new JSONObject().put(FROM, "09:00").put(TO, "09:30"));
        return createRecurringBody;
    }
}


