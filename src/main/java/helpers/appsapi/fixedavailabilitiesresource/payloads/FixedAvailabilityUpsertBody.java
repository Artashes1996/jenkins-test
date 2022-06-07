package helpers.appsapi.fixedavailabilitiesresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FixedAvailabilityUpsertBody {

    public static final String DATE = "date";
    public static final String RESOURCE_ID = "resourceId";
    public static final String TIME_SLOTS = "timeSlots";
    public static final String FROM = "from";
    public static final String TO = "to";

    public JSONObject bodyBuilder(final String resourceId) {
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "10:00");
        timeSlot.put(TO, "11:00");

        final JSONArray timeSlots = new JSONArray();
        timeSlots.put(timeSlot);

        final JSONObject upsertBody = new JSONObject();
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        upsertBody.put(DATE, date);
        upsertBody.put(RESOURCE_ID, resourceId);
        upsertBody.put(TIME_SLOTS, timeSlots);

        return upsertBody;
    }

}
