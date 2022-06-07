package helpers.appsapi.locationservicelinksresource.payloads;

import org.json.JSONObject;

import static utils.TestUtils.*;

public class LocationServiceLinksBody {

    public static final String DESTINATION_GROUP_ID = "destinationGroupId";
    public static final String DURATION = "duration";
    public static final String RESOURCE_SELECTION = "resourceSelection";
    public static final String VISIBILITY = "visibility";
    public static final String MONITOR = "monitor";
    public static final String PHYSICAL_KIOSK = "physicalKiosk";
    public static final String WEB_KIOSK = "webKiosk";

    public enum ResourceSelection {
        DISABLED,
        ALLOWED,
        REQUIRED
    }

    public final JSONObject bodyBuilder() {
        final JSONObject serviceCreationBody = new JSONObject();
        final JSONObject visibilityBody = new JSONObject();

        serviceCreationBody.put(DESTINATION_GROUP_ID, JSONObject.NULL);
        serviceCreationBody.put(RESOURCE_SELECTION, getRandomEnumByClass(ResourceSelection.class).name());

        final int serviceDurationMinValue = 300;
        serviceCreationBody.put(DURATION, getRandomInt(600) + serviceDurationMinValue);

        visibilityBody.put(MONITOR, RANDOM.nextBoolean());
        visibilityBody.put(PHYSICAL_KIOSK, RANDOM.nextBoolean());
        visibilityBody.put(WEB_KIOSK, RANDOM.nextBoolean());
        serviceCreationBody.put(VISIBILITY, visibilityBody);
        return serviceCreationBody;
    }

}
