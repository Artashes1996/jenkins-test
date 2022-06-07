package helpers.appsapi.servicesresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;
import static utils.TestUtils.*;

import java.util.*;
import java.util.function.Supplier;

import static utils.TestUtils.getRandomInt;

public class ServiceUpdateRequestBody {
    public static Map<ServiceUpdateCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static final String NAME_TRANSLATION = "nameTranslation";
    public static final String DURATION = "duration";
    public static final String INTERNAL_NAME = "internalName";
    public static final String RESOURCE_SELECTION = "resourceSelection";
    public static final String STATUS = "status";
    public static final String VISIBILITY = "visibility";
    public static final String MONITOR = "monitor";
    public static final String PHYSICAL_KIOSK = "physicalKiosk";
    public static final String WEB_KIOSK = "webKiosk";
    public static final String FIELD_LINK_CREATION_REQUESTS = "fieldLinkCreationRequests";
    public static final String FIELD_LINK_ID_TO_DELETE = "fieldLinkIdsToDelete";
    public static final String FIELD_LINK_MODIFICATION_REQUEST = "fieldLinkModificationRequests";
    public static final String DISPLAY_TO = "displayTo";
    public static final String FIELD_ID = "fieldId";
    public static final String OPTIONAL = "optional";
    public static final String ORDER = "order";
    public static final String ID = "id";


    public enum ServiceUpdateCombination {
        ALL_FIELDS,
        RANDOM_FIELDS,
        REQUIRED
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public enum ResourceSelection {
        DISABLED,
        ALLOWED,
        REQUIRED
    }

    public enum DisplayTo {
        EVERYONE,
        STAFF_ONLY
    }

    private final Supplier<JSONObject> basic = () -> {
        final JSONObject updateBody = new JSONObject();
        updateBody.put(INTERNAL_NAME, UUID.randomUUID() + " Internal Name");
        updateBody.put(NAME_TRANSLATION, UUID.randomUUID() + " Display Name");
        final int duration = new Random().nextInt(600) + 300;

        updateBody.put(DURATION, duration);
        updateBody.put(STATUS, Status.ACTIVE.name());
        return  updateBody;
    };

    private final Supplier<JSONObject> required = () -> {
        final JSONObject updateBody = basic.get();

        final JSONObject visibility = new JSONObject();
        updateBody.put(RESOURCE_SELECTION, ResourceSelection.ALLOWED.name());
        visibility.put(MONITOR, true);
        visibility.put(PHYSICAL_KIOSK, true);
        visibility.put(WEB_KIOSK, true);
        updateBody.put(VISIBILITY, visibility);
        return updateBody;
    };

    private final Supplier<JSONObject> randomFields = () -> {
        final JSONObject updateBody = basic.get();

        final JSONObject visibility = new JSONObject();
        updateBody.put(RESOURCE_SELECTION, Arrays.asList(ResourceSelection.values()).get(getRandomInt(ResourceSelection.values().length)).name());
        visibility.put(MONITOR, RANDOM.nextBoolean());
        visibility.put(PHYSICAL_KIOSK, RANDOM.nextBoolean());
        visibility.put(WEB_KIOSK, RANDOM.nextBoolean());
        updateBody.put(VISIBILITY, visibility);
        return updateBody;
    };

    private final Supplier<JSONObject> allFields = () -> {
        final JSONObject updateBody = required.get();

        final JSONArray fieldLinkCreationRequest = new JSONArray();
        final JSONArray fieldLinkIdsToDelete = new JSONArray();
        final JSONArray fieldLinkModificationRequest = new JSONArray();
        updateBody.put(FIELD_LINK_CREATION_REQUESTS, fieldLinkCreationRequest);
        updateBody.put(FIELD_LINK_ID_TO_DELETE, fieldLinkIdsToDelete);
        updateBody.put(FIELD_LINK_MODIFICATION_REQUEST, fieldLinkModificationRequest);

        return updateBody;
    };

    public JSONObject bodyBuilder(ServiceUpdateCombination mode){
        return MAP.get(mode).get();
    }

    {
        MAP.put(ServiceUpdateCombination.REQUIRED, required);
        MAP.put(ServiceUpdateCombination.RANDOM_FIELDS, randomFields);
        MAP.put(ServiceUpdateCombination.ALL_FIELDS, allFields);
    }
}
