package helpers.appsapi.servicesresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.commons.ToggleAction;

public class ToggleLinkLocationsToServiceRequestBody {

    public static final String ACTION = "action";
    public static final String LOCATION_IDS = "locationIds";

    public static JSONObject bodyBuilder(ToggleAction action, JSONArray locationIds) {
        final JSONObject toggleActionBody = new JSONObject();
        toggleActionBody.put(ACTION, action.name());
        toggleActionBody.put(LOCATION_IDS, locationIds);
        return toggleActionBody;
    }
}
