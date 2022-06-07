package helpers.appsapi.organizationsresource.payloads;

import org.json.JSONObject;

import java.util.UUID;

import static utils.TestUtils.FAKER;

public class UpdateOrganizationRequestBody {

    public final static String PHONE_NUMBER = "phoneNumber";
    public final static String WEBSITE_URL = "websiteUrl";
    public final static String INTERNAL_NAME = "internalName";

    public static JSONObject bodyBuilder() {
        final JSONObject body = new JSONObject();
        body.put(PHONE_NUMBER, "+37477889900");
        body.put(WEBSITE_URL, "https://www.qless.qa");
        body.put(INTERNAL_NAME, FAKER.company().name() + UUID.randomUUID());
        return body;
    }

}
