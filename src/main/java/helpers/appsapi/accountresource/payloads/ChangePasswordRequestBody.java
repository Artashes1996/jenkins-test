package helpers.appsapi.accountresource.payloads;

import org.json.JSONObject;

public class ChangePasswordRequestBody {

    public static final String CURRENT_PASSWORD = "currentPassword";
    public static final String NEW_PASSWORD = "newPassword";

    public static JSONObject bodyBuilder() {
        final JSONObject requestBody = new JSONObject();
        requestBody.put(CURRENT_PASSWORD, "Qw!123456");
        requestBody.put(NEW_PASSWORD, "NewQw!123456");
        return requestBody;
    }

}
