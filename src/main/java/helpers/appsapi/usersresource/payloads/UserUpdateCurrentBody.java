package helpers.appsapi.usersresource.payloads;

import org.json.JSONObject;

import java.util.Random;

public class UserUpdateCurrentBody {

    public final static String CONTACT_NUMBER = "contactNumber";
    public final static String FIRST_NAME = "firstName";
    public final static String LAST_NAME = "lastName";

    public static JSONObject bodyBuilder(){
        final JSONObject details = new JSONObject();
        details.put(FIRST_NAME, "FirstName " + new Random().nextInt());
        details.put(LAST_NAME, "LastName " + new Random().nextInt());
        details.put(CONTACT_NUMBER, "+37455677318");

        return details;
    }
}
