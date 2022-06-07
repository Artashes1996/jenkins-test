package helpers.appsapi.fixedavailabilitiesresource.payloads;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FixedAvailabilitySearchBody {

    public static final String RESOURCE_ID = "resourceId";
    public static final String FROM = "from";
    public static final String TO = "to";

    public JSONObject bodyBuilder(final String resourceId) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(FROM, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().minusYears(1)));
        searchBody.put(TO, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(2)));
        searchBody.put(RESOURCE_ID, resourceId);

        return searchBody;
    }
}
