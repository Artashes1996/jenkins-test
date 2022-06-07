package helpers.appsapi.resourcesresource.payloads;

import helpers.flows.LocationFlows;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;
import java.util.function.Supplier;

import static utils.TestUtils.FAKER;

public class ResourceCreationBody {

    public final static String INTERNAL_NAME = "internalName";
    public final static String LOCATION_IDS = "locationIds";
    public final static String NAME_TRANSLATION = "nameTranslation";
    public final static String STATUS = "status";

    public enum RESOURCE_CREATION_COMBINATION {
        REQUIRED,
        ALL_FIELDS
    }

    public enum STATUSES {
        ACTIVE,
        INACTIVE
    }

    private final Supplier<JSONObject> requiredFields = () -> {

        final JSONObject body = new JSONObject();
        body.put(NAME_TRANSLATION,  "Resource " + FAKER.ancient().god());
        body.put(INTERNAL_NAME, FAKER.ancient().god() + " " + UUID.randomUUID());
        body.put(STATUS, STATUSES.ACTIVE);
        return body;
    };

    public JSONObject bodyBuilder(RESOURCE_CREATION_COMBINATION combination, String organizationUuid) {
        final JSONObject body = requiredFields.get();

        if(combination.equals(RESOURCE_CREATION_COMBINATION.ALL_FIELDS)){
            final JSONArray locationsIds = new JSONArray();
            locationsIds.put(new LocationFlows().createLocation(organizationUuid));
            body.put(LOCATION_IDS, locationsIds);
        }
        return body;
    }

}
