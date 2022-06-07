package helpers.appsapi.appointmentsresource.payloads;

import org.json.JSONObject;

public class GetListOfResourceUsersRequestBody {

    public static final String QUERY = "query";
    public static final String SERVICE_ID = "serviceId";

    public JSONObject bodyBuilder(String serviceId) {
        final JSONObject body = new JSONObject();
        body.put(SERVICE_ID, serviceId);
        return body;
    }
}
