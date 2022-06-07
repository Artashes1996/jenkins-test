package helpers.appsapi.resourcesresource.payloads;

import org.json.JSONObject;

public class SearchServiceWithLocationResourceLinkAwareBody {

    public static final String PAGINATION = "pagination";
    public final static String PAGE = "page";
    public final static String SIZE = "size";
    public final static String SORT = "sort";
    public final static String QUERY = "query";


    public enum SearchServiceWithLocationResourceLinkAwareCombination {
        REQUIRED,
        WITH_PAGINATION,
        LINKED_ASC,
        LINKED_DESC,
        QUERY,
        UNLINKED_DESC;
    }

    public static JSONObject bodyBuilder(SearchServiceWithLocationResourceLinkAwareBody.SearchServiceWithLocationResourceLinkAwareCombination combination) {
        final JSONObject searchBody = new JSONObject();
        final JSONObject pagination = new JSONObject();
        switch (combination) {
            case REQUIRED:
                break;
            case WITH_PAGINATION:
                pagination.put(PAGE, 0);
                pagination.put(SIZE, 100);
                searchBody.put(PAGINATION, pagination);
                break;
            case LINKED_ASC:
                pagination.put(SORT, "LINKED:ASC");
                searchBody.put(PAGINATION,pagination);
                break;
            case LINKED_DESC:
                pagination.put(SORT, "LINKED:DESC");
                searchBody.put(PAGINATION,pagination);
                break;
            case UNLINKED_DESC:
                pagination.put(SORT, "UNLINKED:DESC");
                searchBody.put(PAGINATION,pagination);
                break;
            case QUERY:
                pagination.put(QUERY, "name" );
                searchBody.put(PAGINATION,pagination);
                break;
        }
        return searchBody;
    }
}


