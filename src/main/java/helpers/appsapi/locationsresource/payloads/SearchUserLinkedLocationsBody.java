package helpers.appsapi.locationsresource.payloads;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class SearchUserLinkedLocationsBody {
    private final Map<SearchUserLinkedLocationsBody.SearchUserLinkedLocationsCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    public static final String LOCATION_IDS = "locationIds";
    public static final String QUERY = "query";

    public enum SearchUserLinkedLocationsCombination {
        EMPTY_BODY,
        WITH_LOCATIONS,
        WITH_QUERY
    }

    private final Supplier<JSONObject> emptyBody = JSONObject::new;

    private final Supplier<JSONObject> withQuery = () -> {
        final JSONObject searchBody = emptyBody.get();
        searchBody.put(QUERY, "");
        return searchBody;
    };

    private final Supplier<JSONObject> withLocations = () -> {
        final JSONObject searchBody = emptyBody.get();
        final JSONArray locationsIds = new JSONArray();
        searchBody.put(LOCATION_IDS, locationsIds);
        return searchBody;
    };

    {
        MAP.put(SearchUserLinkedLocationsCombination.EMPTY_BODY, emptyBody);
        MAP.put(SearchUserLinkedLocationsCombination.WITH_QUERY, withQuery);
        MAP.put(SearchUserLinkedLocationsCombination.WITH_LOCATIONS, withLocations);
    }

    public JSONObject bodyBuilder(SearchUserLinkedLocationsCombination combination) {
        return MAP.get(combination).get();
    }

}