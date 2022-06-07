package helpers.appsapi.servicesresource.payloads;

import org.json.JSONObject;

import java.util.UUID;

import static utils.TestUtils.FAKER;
import static utils.TestUtils.getRandomInt;

public class ServiceCreationRequestBody {

    public static final String DISPLAY_NAME = "displayName";
    public static final String DURATION = "duration";
    public static final String INTERNAL_NAME = "internalName";
    public static final String RESOURCE_SELECTION = "resourceSelection";
    public static final String STATUS = "status";
    public static final String VISIBILITY = "visibility";
    public static final String MONITOR = "monitor";
    public static final String PHYSICAL_KIOSK = "physicalKiosk";
    public static final String WEB_KIOSK = "webKiosk";
    public static final String FIELD_LINK_CREATION_REQUEST = "fieldLinkCreationRequest";
    public static final String DISPLAY_TO = "displayTo";
    public static final String FIELD_ID = "fieldId";
    public static final String OPTIONAL = "optional";
    public static final String ORDER = "order";

    public enum ResourceSelection {
        DISABLED,
        ALLOWED,
        REQUIRED
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public static JSONObject bodyBuilder() {
        final JSONObject serviceCreationBody = new JSONObject();
        final JSONObject visibilityBody = new JSONObject();
        serviceCreationBody.put(INTERNAL_NAME, FAKER.lordOfTheRings().location() + " " + UUID.randomUUID());
        serviceCreationBody.put(DISPLAY_NAME,  FAKER.lordOfTheRings().location());
        serviceCreationBody.put(RESOURCE_SELECTION, ResourceSelection.ALLOWED.name());
        serviceCreationBody.put(STATUS, Status.ACTIVE);
        final int serviceDurationMinValue = 300;
        serviceCreationBody.put(DURATION, getRandomInt(600) + serviceDurationMinValue);
        visibilityBody.put(MONITOR, true);
        visibilityBody.put(PHYSICAL_KIOSK, true);
        visibilityBody.put(WEB_KIOSK, true);
        serviceCreationBody.put(VISIBILITY, visibilityBody);

        return serviceCreationBody;
    }

}
