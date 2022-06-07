package helpers.appsapi.resourcesresource.payloads;

import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SearchWithResourceLinkAwareBody {
    private final static Map<SearchLocationResourceLinkAware, Supplier<JSONObject>> MAP = new HashMap<>();
    public final static String INCLUDE_DELETED = "includeDeleted";
    public final static String LOCATION_IDS = "locationIds";
    public final static String PAGINATION = "pagination";
    public final static String PAGE = "page";
    public final static String SIZE = "size";
    public final static String SORT = "sort";
    public final static String QUERY = "query";

    public enum SearchLocationResourceLinkAware {
        REQUIRED,
        PAGINATION,
        ALL_FIELDS
    }

    @Getter
    public enum SortingBy {
        NAME("INTERNAL_NAME:ASC", "INTERNAL_NAME:DESC"),
        ID("ID:ASC", "ID:DESC"),
        CREATION_DATE("CREATION_DATE:ASC", "CREATION_DATE:DESC"),
        FIRST_NAME("FIRST_NAME:ASC", "FIRST_NAME:DESC"),
        LAST_NAME("LAST_NAME:ASC", "LAST_NAME:DESC"),
        EMAIL("EMAIL:ASC", "EMAIL:DESC"),
        USER_STATUS("USER_STATUS:ASC", "USER_STATUS:DESC");
        private final String ascending;
        private final String descending;

        SortingBy(String ascending, String descending) {
            this.ascending = ascending;
            this.descending = descending;
        }
    }

    private final Supplier<JSONObject> empty = JSONObject::new;

    private final Supplier<JSONObject> pagination = () -> {
        final JSONObject searchBody = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(PAGE, 0);
        pagination.put(SIZE, 50);
        pagination.put(SORT, SortingBy.NAME.getAscending());
        searchBody.put(PAGINATION, pagination);
        return searchBody;
    };

    private final Supplier<JSONObject> allFields = () -> {
        final JSONObject searchBody = pagination.get();
        searchBody.put(INCLUDE_DELETED, true);
        final JSONArray locationIds = new JSONArray();
        searchBody.put(LOCATION_IDS, locationIds);
        searchBody.put(QUERY, "");
        return searchBody;
    };

    public JSONObject bodyBuilder(final SearchLocationResourceLinkAware mode){
        return MAP.get(mode).get();
    }

    {
        MAP.put(SearchLocationResourceLinkAware.REQUIRED, empty);
        MAP.put(SearchLocationResourceLinkAware.PAGINATION, pagination);
        MAP.put(SearchLocationResourceLinkAware.ALL_FIELDS, allFields);
    }

}
