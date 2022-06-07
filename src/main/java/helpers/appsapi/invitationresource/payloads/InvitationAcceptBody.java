package helpers.appsapi.invitationresource.payloads;

import org.json.JSONObject;

import static utils.TestUtils.*;

public class InvitationAcceptBody {

    public final static String PASSWORD = "password";
    public final static String TOKEN = "token";
    public final static String LAST_NAME = "lastName";
    public final static String FIRST_NAME = "firstName";
    public final static String CONTACT_NUMBER = "contactNumber";

    public JSONObject bodyBuilder() {
        final JSONObject acceptBody = new JSONObject();
        acceptBody.put(CONTACT_NUMBER, getRandomPhoneNumber());
        acceptBody.put(FIRST_NAME, FAKER.name().firstName());
        acceptBody.put(LAST_NAME, FAKER.name().lastName());
        acceptBody.put(PASSWORD, "Qw!123456");
        return acceptBody;
    }

}