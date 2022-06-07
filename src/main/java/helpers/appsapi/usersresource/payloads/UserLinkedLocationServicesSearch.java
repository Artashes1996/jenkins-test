package helpers.appsapi.usersresource.payloads;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class UserLinkedLocationServicesSearch {

    private static final Map<UserLinkedLocationCombination, Supplier<JSONObject>> MAP = new HashMap<>();

    public static final String PAGINATION = "pagination";
    public static final String SIZE = "size";
    public static final String PAGE = "page";
    public static final String SORT = "sort";
    public static final String LINKED = "linked";
    public static final String QUERY = "query";


    public enum UserLinkedLocationCombination {
        REQUIRED_FIELDS,
        WITH_PAGINATION
    }

    private final Supplier<JSONObject> requiredFields = JSONObject::new;

    private final Supplier<JSONObject> withPagination = () -> {

        final JSONObject searchBody = requiredFields.get();
        final JSONObject pagination = new JSONObject();
        pagination.put(PAGE, 0);
        pagination.put(SIZE, 50);
        searchBody.put(PAGINATION, pagination);

        return searchBody;
    };


    public JSONObject bodyBuilder(UserLinkedLocationCombination combination) {
        return MAP.get(combination).get();

    }

    {
        MAP.put(UserLinkedLocationCombination.REQUIRED_FIELDS, requiredFields);
        MAP.put(UserLinkedLocationCombination.WITH_PAGINATION, withPagination);
    }

}
