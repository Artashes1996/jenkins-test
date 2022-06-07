package helpers.appsapi.locationsresource.payloads;

import lombok.Getter;
import org.json.JSONObject;

public class LocationsSearchRequestBody {

    public static final String PAGINATION = "pagination";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String SORT = "sort";
    public static final String QUERY = "query";
    public static final String SEARCH_MODE = "searchMode";
    public static final String WORKING_USER_ID = "workingUserId";

    public enum LocationSearchModes {
        NO_FILTERING,
        FILTERED_BY_SEARCH_LOCATIONS_PERMISSION,
        FILTERED_BY_LOCATION_USER_SERVICE_LINK_PERMISSION
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

    public static JSONObject bodyBuilder() {
        final JSONObject searchBody = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(PAGE, 0);
        pagination.put(SIZE, 1);
        pagination.put(SORT, SortingBy.NAME.getAscending());
        searchBody.put(PAGINATION, pagination);
        searchBody.put(QUERY, "");

        return searchBody;
    }
}