package helpers.appsapi.servicesresource.payloads;

public class LinkAwareServiceLocationsSearchBody {

    public static final String CITY = "city";
    public static final String INCLUDE_DELETED = "includeDeleted";
    public static final String PAGINATION = "pagination";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String SORT = "sort";
    public static final String QUERY = "query";
    public static final String STATE_REGION = "stateRegion";
    public static final String STATUSES = "stateRegion";
    public static final String TYPES = "types";
    public static final String ZIPCODE = "zipcode";

    public enum SortDirection {
        ASC,
        DESC
    }
}
