package helpers.appsapi.resourcesresource.payloads;

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SearchResourceRequestBody {

    private static final Map<ResourceSearchCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static final String LOCATION_IDS = "locationIds";
    public static final String PAGINATION = "pagination";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String SORT = "sort";
    public static final String QUERY = "query";
    public static final String TYPES = "types";
    public static final String NOT_LINKED_TO_ANY_LOCATION = "notLinkedToAnyLocation";

    @Getter
    public enum SortingBy {
        INTERNAL_NAME("INTERNAL_NAME:ASC", "INTERNAL_NAME:DESC");
        private final String ascending;
        private final String descending;

        SortingBy(String ascending, String descending) {
            this.ascending = ascending;
            this.descending = descending;
        }
    }

    public enum ResourceTypes {
        OTHER,
        EMPLOYEE
    }

    public enum ResourceSearchCombination {
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
        searchBody.put(TYPES, new JSONArray());
        searchBody.put(QUERY, "");
        searchBody.put(NOT_LINKED_TO_ANY_LOCATION, false);
        return searchBody;
    };

    public JSONObject bodyBuilder(ResourceSearchCombination combination) {
        return MAP.get(combination).get();
    }

    {
        MAP.put(ResourceSearchCombination.REQUIRED_FIELDS, requiredFields);
        MAP.put(ResourceSearchCombination.WITH_PAGINATION, withPagination);
        MAP.put(ResourceSearchCombination.ALL_FIELDS, allFields);
    }

}
