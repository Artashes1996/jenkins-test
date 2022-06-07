package helpers.appsapi.servicesresource.payloads;

import lombok.Getter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ServiceSearchRequestBody {

    private static final Map<SearchServiceLocationCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static final String PAGINATION = "pagination";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String SORT = "sort";
    public static final String QUERY = "query";
    public static final String LOCATION_ID = "locationId";

    @Getter
    public enum SortingBy {
        INTERNAL_NAME("INTERNAL_NAME:ASC", "INTERNAL_NAME:DESC"),
        ID("ID:ASC", "ID:DESC");
        private final String ascending;
        private final String descending;

        SortingBy(String ascending, String descending) {
            this.ascending = ascending;
            this.descending = descending;
        }
    }

    public enum SearchServiceLocationCombination{
        REQUIRED_FIELDS,
        ALL_FIELDS,
        WITH_PAGINATION
    }

    private static final Supplier<JSONObject> requiredFields = JSONObject::new;

    private static final Supplier<JSONObject> withPagination = () -> {
        final JSONObject searchBody = requiredFields.get();
        searchBody.put(PAGINATION, new JSONObject());
        return searchBody;
    };

    private static final Supplier<JSONObject> allFields = () -> {
        final JSONObject searchBody = withPagination.get();
        searchBody.getJSONObject(PAGINATION).put(PAGE, 0);
        searchBody.getJSONObject(PAGINATION).put(SIZE, 50);
        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.INTERNAL_NAME.ascending);

        return searchBody;
    };

    static {
        MAP.put(SearchServiceLocationCombination.REQUIRED_FIELDS, requiredFields);
        MAP.put(SearchServiceLocationCombination.WITH_PAGINATION, withPagination);
        MAP.put(SearchServiceLocationCombination.ALL_FIELDS, allFields);
    }

    public static JSONObject bodyBuilder(SearchServiceLocationCombination combination){
        return MAP.get(combination).get();
    }
}
