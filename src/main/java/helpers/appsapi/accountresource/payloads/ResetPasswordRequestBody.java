package helpers.appsapi.accountresource.payloads;

import org.json.JSONObject;

public class ResetPasswordRequestBody {
    public static final String EMAIL = "email";

    public static JSONObject bodyBuilder(String email) {
        final JSONObject requestBody = new JSONObject();
        requestBody.put(EMAIL, email);
        return requestBody;
    }
}
