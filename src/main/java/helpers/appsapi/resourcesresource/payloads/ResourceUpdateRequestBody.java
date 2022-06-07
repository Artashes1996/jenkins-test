package helpers.appsapi.resourcesresource.payloads;

import org.json.JSONObject;
import pages.UsersListPage;

import static utils.TestUtils.FAKER;

public class ResourceUpdateRequestBody {

    public static final String NAME_TRANSLATION = "nameTranslation";
    public static final String STATUS = "status";

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    public JSONObject bodyBuilder(Status status) {
        final JSONObject resourceUpdateBody = new JSONObject();
        final String nameTranslation = NAME_TRANSLATION + FAKER.artist().name();
        resourceUpdateBody.put(ResourceUpdateRequestBody.NAME_TRANSLATION, nameTranslation);
        resourceUpdateBody.put(ResourceUpdateRequestBody.STATUS, status.name());
        return resourceUpdateBody;
    }
}
