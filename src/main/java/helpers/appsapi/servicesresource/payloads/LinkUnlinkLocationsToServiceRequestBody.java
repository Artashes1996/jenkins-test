package helpers.appsapi.servicesresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.commons.ToggleAction;

public class LinkUnlinkLocationsToServiceRequestBody {

    public static final String ACTION = "action";
    public static final String LOCATION_IDS = "locationIds";

    public static JSONObject bodyBuilder(ToggleAction action, JSONArray locationsIds){

        final JSONObject body = new JSONObject();
        body.put(ACTION, action);
        body.put(LOCATION_IDS, locationsIds);
        return body;
    }
}
