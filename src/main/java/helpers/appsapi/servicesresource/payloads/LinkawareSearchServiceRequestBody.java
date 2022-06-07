package helpers.appsapi.servicesresource.payloads;

import org.json.JSONObject;

public class LinkawareSearchServiceRequestBody {

    public static final String INCLUDE_DELETED = "includeDeleted";
    public static final String PAGINATION = "pagination";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String SORT = "sort";
    public static final String QUERY = "query";
    public static final String STATUSES = "statuses";
    public static final String TYPES = "types";
    public static final String CITIES = "cities";
    public static final String STATE_REGIONS = "stateRegions";
    public static final String ZIPCODES = "zipcodes";
    public static final String LOCATION_IDS = "locationIds";

    public enum SortDirection {
        ASC,
        DESC
    }

    public static JSONObject bodyBuilder() {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(INCLUDE_DELETED, true);
        final JSONObject pagination = new JSONObject();
        pagination.put(PAGE, 0);
        pagination.put(SIZE, 1);
        pagination.put(SORT, "INTERNAL_NAME:" + SortDirection.ASC);
        searchBody.put(PAGINATION, pagination);
        searchBody.put(QUERY, "");

        return searchBody;
    }
}
