package helpers.appsapi.locationsresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;
import utils.commons.ToggleAction;

public class ToggleLinkServicesToLocationRequestBody {
    public static final String ACTION = "action";
    public static final String SERVICE_IDS = "serviceIds";

    public static JSONObject bodyBuilder(ToggleAction action, JSONArray serviceIds) {
        final JSONObject toggleActionBody = new JSONObject();
        toggleActionBody.put(ACTION, action.name());
        toggleActionBody.put(SERVICE_IDS, serviceIds);
        return toggleActionBody;
    }

}
