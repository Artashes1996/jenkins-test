package helpers.appsapi.locationservicegroupsresource.payloads;

import org.json.JSONObject;

import static utils.TestUtils.FAKER;

public class GroupUpdateBody {

    public static final String DISPLAY_NAME = "displayName";
    public static final String NAME = "name";

    public static JSONObject bodyBuilder() {
        final JSONObject groupCreationBody = new JSONObject();
        groupCreationBody.put(DISPLAY_NAME, FAKER.gameOfThrones().house());
        groupCreationBody.put(NAME, FAKER.ancient().god());
        return groupCreationBody;
    }
}
