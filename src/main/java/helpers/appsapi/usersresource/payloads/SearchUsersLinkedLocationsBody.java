package helpers.appsapi.usersresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SearchUsersLinkedLocationsBody {

    private static final Map<UserLinkedLocationCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static final String INCLUDE_DELETED = "includeDeleted";
    public static final String PAGINATION = "pagination";
    public static final String SIZE = "size";
    public static final String PAGE = "page";
    public static final String SORT = "sort";
    public static final String STATUSES = "statuses";
    public static final String TYPES = "types";
    public static final String LOCATION_IDS = "locationIds";


    public enum UserLinkedLocationCombination {
        REQUIRED_FIELDS,
        WITH_PAGINATION,
        ALL_FIELDS
    }

    private final Supplier<JSONObject> requiredFields = JSONObject::new;

    private final Supplier<JSONObject> withPagination = () -> {

        final JSONObject searchBody = requiredFields.get();
        searchBody.put(PAGINATION, new JSONObject());

        return searchBody;
    };

    private final Supplier<JSONObject> allFields = () -> {

        final JSONObject searchBody = withPagination.get();
        searchBody.getJSONObject(PAGINATION).put(PAGE, 0);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 50);
        searchBody.put(INCLUDE_DELETED, false);
        searchBody.put(STATUSES, new JSONArray().put("ACTIVE"));
        searchBody.put(TYPES, new JSONArray().put("VIRTUAL"));

        return searchBody;
    };

    public JSONObject bodyBuilder(UserLinkedLocationCombination combination) {
        return MAP.get(combination).get();

    }

    {
        MAP.put(UserLinkedLocationCombination.REQUIRED_FIELDS, requiredFields);
        MAP.put(UserLinkedLocationCombination.WITH_PAGINATION, withPagination);
        MAP.put(UserLinkedLocationCombination.ALL_FIELDS, allFields);
    }

}
