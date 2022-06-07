package helpers.appsapi.locationservicegroupsresource.payloads;

import org.json.JSONObject;

import static utils.TestUtils.*;

public class GroupCreationBody {

    public static final String DISPLAY_NAME = "displayName";
    public static final String NAME = "name";
    public static final String PARENT_ID = "parentId";

    public static JSONObject bodyBuilder() {
        final JSONObject groupCreationBody = new JSONObject();
        groupCreationBody.put(DISPLAY_NAME, FAKER.gameOfThrones().house());
        groupCreationBody.put(NAME, FAKER.pokemon().name());
        return groupCreationBody;
    }

}
