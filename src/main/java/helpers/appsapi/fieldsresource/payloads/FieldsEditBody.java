package helpers.appsapi.fieldsresource.payloads;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static helpers.appsapi.fieldsresource.payloads.FieldTypes.*;
import static utils.TestUtils.getRandomInt;

public class FieldsEditBody {

    public Map<FieldTypes, Supplier<JSONObject>> MAP = new HashMap<>();

    public static final String DISPLAY_NAME = "displayName";
    public static final String INTERNAL_NAME = "internalName";
    public static final String OPTIONS_TO_CREATE = "optionsToCreate";
    public static final String OPTION_CREATION_INTERNAL_NAME = "internalName";

    public static final String OPTION_IDS_TO_DELETE = "optionIdsToDelete";
    public static final String OPTIONS_TO_MODIFY = "optionsToModify";

    public static final String OPTION_MODIFICATION_ID = "id";
    public static final String OPTION_MODIFICATION_INTERNAL_NAME = "internalName";

    public static final String REGEX = "regex";

    private final Supplier<JSONObject> numberType = () -> {
        final JSONObject fieldEditBody = new JSONObject();
        final String internalName = getRandomInt() + " field internal";
        final String displayName = getRandomInt() + " field display";
        fieldEditBody.put(INTERNAL_NAME, internalName);
        fieldEditBody.put(DISPLAY_NAME, displayName);
        return fieldEditBody;
    };

    private final Supplier<JSONObject> textType = () -> {
        final JSONObject fieldEditBody = numberType.get();
        final String regex = "^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\\\"(?:[\\\\x01-\\\\x08\\\\x0b\\\\x0c\\\\x0e-\\\\x1f\\\\x21\\\\x23-\\\\x5b\\\\x5d-\\\\x7f]|\\\\\\\\[\\\\x01-\\\\x09\\\\x0b\\\\x0c\\\\x0e-\\\\x7f])*\\\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\\\x01-\\\\x08\\\\x0b\\\\x0c\\\\x0e-\\\\x1f\\\\x21-\\\\x5a\\\\x53-\\\\x7f]|\\\\\\\\[\\\\x01-\\\\x09\\\\x0b\\\\x0c\\\\x0e-\\\\x7f])+)\\\\])$";
        fieldEditBody.put(REGEX, regex);
        return fieldEditBody;
    };

    private final Supplier<JSONObject> checkboxType = () -> {
        final JSONObject fieldEditBody = numberType.get();
        fieldEditBody.put(OPTIONS_TO_CREATE, new JSONArray());
        fieldEditBody.put(OPTION_IDS_TO_DELETE, new JSONArray());
        fieldEditBody.put(OPTIONS_TO_MODIFY, new JSONArray());
        return fieldEditBody;
    };

    private final Supplier<JSONObject> radioButtonType = checkboxType::get;

    private final Supplier<JSONObject> singleSelectType = checkboxType::get;

    private final Supplier<JSONObject> multiSelectType = checkboxType::get;

    {
        MAP.put(NUMBER, numberType);
        MAP.put(TEXT, textType);
        MAP.put(CHECKBOX, checkboxType);
        MAP.put(RADIOBUTTON, radioButtonType);
        MAP.put(SINGLE_SELECT_DROPDOWN, singleSelectType);
        MAP.put(MULTI_SELECT_DROPDOWN, multiSelectType);
    }

    public JSONObject bodyBuilder(FieldTypes fieldType){
        return MAP.get(fieldType).get();
    }

}
