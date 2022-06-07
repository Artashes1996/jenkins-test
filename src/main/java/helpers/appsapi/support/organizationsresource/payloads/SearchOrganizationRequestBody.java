package helpers.appsapi.support.organizationsresource.payloads;

import org.json.JSONObject;

public class SearchOrganizationRequestBody {

    public final static String PAGINATION = "pagination";
    public final static String PAGE = "page";
    public final static String SIZE = "size";
    public final static String SORT = "sort";
    public final static String DELETED = "deleted";
    public final static String QUERY = "query";

    public enum OrganizationSearchCombination {

        DEFAULT,
        WITH_EMPTY_PAGINATION,
        PAGE_ONLY,
        SIZE_ONLY,
        WITH_FULL_PAGINATION,
        PAGINATION_AND_SORTING,
        QUERY_ONLY,
        DELETED_ONLY,
        FULL
    }

    public enum OrganizationSortingKey {

        DELETION_DATE,
        WEBSITE_URL,
        ID,
        NUMBER_OF_LOCATIONS,
        NUMBER_OF_USERS,
        INTERNAL_NAME
    }

    public enum SortDirection {
        ASC,
        DESC
    }

    public static String getTranslation(String key){
        switch (key) {
            case "DELETION_DATE":
                return "deletionDate";
            case "WEBSITE_URL":
                return "websiteUrl";
            case "ID":
                return "id";
            case "NUMBER_OF_LOCATIONS":
                return "numberOfLocations";
            case "INTERNAL_NAME":
                return "internalName";
            default : return null;
        }
    }

    public static JSONObject bodyBuilder(OrganizationSearchCombination combination) {
        final JSONObject body;
        final JSONObject pagination;

        switch(combination) {

            case DEFAULT: {
                body = new JSONObject();
                break;
            }

            case WITH_EMPTY_PAGINATION: {
                body = new JSONObject();
                pagination = new JSONObject();
                body.put(PAGINATION, pagination);
                break;
            }

            case PAGE_ONLY: {
                body = new JSONObject();
                pagination = new JSONObject();
                pagination.put(PAGE,0);
                body.put(PAGINATION, pagination);
                break;
            }

            case SIZE_ONLY: {
                body = new JSONObject();
                pagination = new JSONObject();
                pagination.put(SIZE,1);
                body.put(PAGINATION, pagination);
                break;
            }

            case DELETED_ONLY: {
                body = new JSONObject();
                body.put(DELETED, false);
                break;
            }

            case WITH_FULL_PAGINATION: {
                body = new JSONObject();
                pagination = new JSONObject();
                pagination.put(SIZE,1);
                pagination.put(PAGE,0);
                body.put(PAGINATION, pagination);
                break;
            }

            case PAGINATION_AND_SORTING: {
                body = new JSONObject();
                pagination = new JSONObject();
                pagination.put(SIZE,1);
                pagination.put(PAGE,0);
                pagination.put(SORT, OrganizationSortingKey.INTERNAL_NAME + ":" + SortDirection.ASC );
                body.put(PAGINATION, pagination);
                break;
            }

            case QUERY_ONLY: {
                body = new JSONObject();
                body.put(QUERY, "QA");
                break;
            }

            case FULL: {
                body = new JSONObject();
                pagination = new JSONObject();
                pagination.put(SIZE,1);
                pagination.put(PAGE,0);
                pagination.put(SORT, OrganizationSortingKey.ID + ":" + SortDirection.DESC );
                body.put(PAGINATION, pagination);
                body.put(QUERY, "Test");
                body.put(DELETED, JSONObject.NULL);
                break;
            }

            default: {
                body = new JSONObject();
            }
        }
        return body;
    }

}
