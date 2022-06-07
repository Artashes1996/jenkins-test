package helpers.appsapi.fieldsresource.payloads;

import lombok.Getter;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FieldsSearchBody {

    private final Map<FieldsSearchCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public final static String PAGINATION = "pagination";
    public final static String PAGE = "page";
    public final static String SIZE = "size";
    public final static String SORT = "sort";
    public final static String QUERY = "query";
    public final static String CUSTOM = "custom";
    public final static String SERVICE_DEFAULTS = "serviceDefaults";

    @Getter
    public enum SortingBy {
        INTERNAL_NAME("INTERNAL_NAME:ASC", "INTERNAL_NAME:DESC"),
        CUSTOM("CUSTOM:ASC", "CUSTOM:DESC");

        private final String ascending;
        private final String descending;

        SortingBy(String ascending, String descending) {
            this.ascending = ascending;
            this.descending = descending;
        }
    }

    public enum FieldsSearchCombination{
        REQUIRED_FIELDS,
        WITH_PAGINATION,
        ALL_FIELDS,
        CUSTOM_FIELDS,
        DEFAULT_FIELDS,
        SERVICE_DEFAULT_FIELDS
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
        searchBody.getJSONObject(PAGINATION).put(SORT, SortingBy.INTERNAL_NAME.ascending);

        return searchBody;
    };

    private final Supplier<JSONObject> withCustomFields = () -> {
        final JSONObject searchBody = requiredFields.get();
        searchBody.put(CUSTOM, true);
        return searchBody;
    };

    private final Supplier<JSONObject> withDefaultFields = () -> {
        final JSONObject searchBody = requiredFields.get();
        searchBody.put(CUSTOM, false);
        return searchBody;
    };

    private final Supplier<JSONObject> withServiceDefaultFields = () -> {
        final JSONObject searchBody = withDefaultFields.get();
        searchBody.put(SERVICE_DEFAULTS, true);
        return searchBody;
    };

    {
        MAP.put(FieldsSearchCombination.REQUIRED_FIELDS, requiredFields);
        MAP.put(FieldsSearchCombination.WITH_PAGINATION, withPagination);
        MAP.put(FieldsSearchCombination.ALL_FIELDS, allFields);
        MAP.put(FieldsSearchCombination.CUSTOM_FIELDS, withCustomFields);
        MAP.put(FieldsSearchCombination.DEFAULT_FIELDS, withDefaultFields);
        MAP.put(FieldsSearchCombination.SERVICE_DEFAULT_FIELDS, withServiceDefaultFields);
    }

    public JSONObject bodyBuilder(FieldsSearchCombination combination){
        return MAP.get(combination).get();
    }

}
