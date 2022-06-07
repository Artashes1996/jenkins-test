package helpers.appsapi.availabletimeslots.payloads;

import helpers.appsapi.support.locationresource.payloads.TimeZones;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.Year;

public class GetAvailableTimeSlotsListBody {

    public static final String MONTH = "month";
    public static final String REPRESENTATION_TIME_ZONE = "representationTimezone";
    public static final String YEAR = "year";
    public static final String RESOURCE_ID = "resourceId";

    public JSONObject bodyBuilder(String resourceId) {
        final JSONObject body = new JSONObject();
        body.put(MONTH, LocalDateTime.now().getMonth());
        body.put(REPRESENTATION_TIME_ZONE, TimeZones.getTimeZoneByUtcOffset(0));
        body.put(YEAR, Year.now().plusYears(1));
        body.put(RESOURCE_ID, resourceId);
        return body;
    }
}
