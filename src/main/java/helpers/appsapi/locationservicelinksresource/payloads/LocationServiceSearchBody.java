package helpers.appsapi.locationservicelinksresource.payloads;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class LocationServiceSearchBody {

    public static Map<SearchLocationServicesCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static String QUERY = "query";
    public static String SORT_BY_NAME = "sortByName";

    public enum SortingBy{
        ASC,
        DESC
    }

    public enum SearchLocationServicesCombination{
        REQUIRED,
        ALL_FIELDS
    }

    private final Supplier<JSONObject> required= JSONObject::new;

    private final Supplier<JSONObject> allFields = () -> {
        final JSONObject searchBody = required.get();
        searchBody.put(QUERY, "");
        searchBody.put(SORT_BY_NAME, SortingBy.ASC);
        return searchBody;
    };

    public JSONObject bodyBuilder(final SearchLocationServicesCombination mode){
        return MAP.get(mode).get();
    }

    {
        MAP.put(SearchLocationServicesCombination.REQUIRED, required);
        MAP.put(SearchLocationServicesCombination.ALL_FIELDS, allFields);
    }

}
