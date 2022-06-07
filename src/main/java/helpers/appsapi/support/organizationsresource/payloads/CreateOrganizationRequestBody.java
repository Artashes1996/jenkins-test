package helpers.appsapi.support.organizationsresource.payloads;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.UUID;

import static utils.TestUtils.*;

public class CreateOrganizationRequestBody {
    public final static String INTERNAL_NAME = "internalName";
    public final static String PHONE_NUMBER = "phoneNumber";
    public final static String VERTICAL = "vertical";
    public final static String WEB_SITE = "websiteUrl";

    public enum OrganizationCreateCombination {
        REQUIRED,
        ALL_FIELDS,
    }

    public enum Vertical {
        EDUCATION,
        GOVERNMENT,
        RETAIL_OTHER,
        HEALTHCARE
    }

    public static JSONObject bodyBuilder(OrganizationCreateCombination combination) {
        final JSONObject invitationBody = new JSONObject();
        final Vertical vertical = Arrays.asList(Vertical.values()).get(getRandomInt(Vertical.values().length));

        invitationBody.put(INTERNAL_NAME, FAKER.company().name() + UUID.randomUUID());
        invitationBody.put(VERTICAL, vertical.name());
        switch (combination) {
            case REQUIRED:
                break;
            case ALL_FIELDS: {
                invitationBody.put(WEB_SITE, "https://sflpro.com/");
                invitationBody.put(PHONE_NUMBER, "+37477889900");
            }

        }
        return invitationBody;
    }
}
