package helpers.appsapi.support.invitationresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

public class InvitationCreationBody {
    public static final String PAYLOADS = "payloads";
    public static final String CONTACT_NUMBER = "contactNumber";
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String SEND_EMAIL = "sendEmail";

    public enum SupportInvitationCreationCombination {
        REQUIRED,
        ALL_FIELDS
    }

    public static JSONObject bodyBuilder(SupportInvitationCreationCombination combination, int count) {
        final JSONObject supportsInvitationBody = new JSONObject();
        final JSONArray payloads;
        switch (combination) {
            case REQUIRED: {
                payloads = new JSONArray();
                for (int i = 0; i < count; i++) {
                    JSONObject payload = new JSONObject();
                    String email = new Random().nextInt() + "@qless.com";
                    payload.put(EMAIL, email);
                    payloads.put(payload);
                }
                supportsInvitationBody.put(PAYLOADS, payloads);
                break;
            }
            case ALL_FIELDS: {
                payloads = new JSONArray();
                for (int i = 0; i < count; i++) {
                    JSONObject payload = new JSONObject();
                    String email = new Random().nextInt() + "@qless.com";
                    String firstName = "Jon";
                    String lastName = "Doe";
                    String contactNumber = "+37477889900";
                    payload.put(EMAIL, email);
                    payload.put(FIRST_NAME, firstName);
                    payload.put(LAST_NAME, lastName);
                    payload.put(CONTACT_NUMBER, contactNumber);
                    payloads.put(payload);
                }
                supportsInvitationBody.put(PAYLOADS, payloads);
                break;
            }
        }
        return supportsInvitationBody;
    }

}
