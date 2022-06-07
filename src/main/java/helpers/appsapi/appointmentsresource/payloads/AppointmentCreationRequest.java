package helpers.appsapi.appointmentsresource.payloads;

import helpers.appsapi.support.locationresource.payloads.TimeZones;
import org.json.JSONArray;
import org.json.JSONObject;

public class AppointmentCreationRequest {

    public static final String CONSUMER_NOTES="consumerNotes";
    public static final String FIELDS = "fields";
    public static final String INTERNAL_NAME = "internalName";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String REPRESENTATION_TIME_ZONE = "representationTimezone";
    public static final String RESOURCE_ID = "resourceId";
    public static final String SOURCE = "source";
    public static final String START_DATE_TIME = "startDateTime";

    public enum AppointmentCreationCombination{
        REQUIRED,
        ALL_FIELDS
    }

    public JSONObject bodyBuilder(String startDateTime, String resourceId){
        final JSONObject appointmentCreationRequestBody = new JSONObject();
        final JSONArray fieldsArray = new JSONArray();
        appointmentCreationRequestBody.put(FIELDS, fieldsArray);
        appointmentCreationRequestBody.put(START_DATE_TIME, startDateTime);
//        TODO move sources to separate enum
        appointmentCreationRequestBody.put(SOURCE, "CONSOLE");
        appointmentCreationRequestBody.put(RESOURCE_ID, resourceId);
        appointmentCreationRequestBody.put(REPRESENTATION_TIME_ZONE, TimeZones.getTimeZoneByUtcOffset(0));

        return appointmentCreationRequestBody;
    }


}
