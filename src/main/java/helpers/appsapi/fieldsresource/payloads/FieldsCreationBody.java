package helpers.appsapi.fieldsresource.payloads;

import org.json.*;
import java.util.*;
import java.util.function.Supplier;

import static helpers.appsapi.fieldsresource.payloads.FieldTypes.*;
import static utils.TestUtils.*;

public class FieldsCreationBody {

    public Map<FieldTypes, Supplier<JSONObject>> MAP = new HashMap<>();

    public static final String DISPLAY_NAME = "displayName";
    public static final String INTERNAL_NAME = "internalName";
    public static final String OPTIONS = "options";
    public static final String REGEX = "regex";
    public static final String TYPE = "type";

    private final Supplier<JSONObject> numberType = () -> {
        final JSONObject fieldCreationBody = new JSONObject();
        final String internalName = FAKER.superhero().name() + UUID.randomUUID();
        final String displayName = getRandomInt() + " field display";
        fieldCreationBody.put(INTERNAL_NAME, internalName);
        fieldCreationBody.put(DISPLAY_NAME, displayName);
        fieldCreationBody.put(TYPE, NUMBER.name());
        return fieldCreationBody;
    };

    private final Supplier<JSONObject> textType = () -> {
        final JSONObject fieldCreationBody = numberType.get();
        final String regex = "[ab]{4,6}c";
        fieldCreationBody.put(REGEX, regex);
        fieldCreationBody.put(TYPE, TEXT.name());
        return fieldCreationBody;
    };

    private final Supplier<JSONObject> checkboxType = () -> {
        final JSONObject fieldCreationBody = numberType.get();
        final JSONArray options = new JSONArray();

        final JSONObject option = new JSONObject();
        final String internalNameForOption = getRandomInt() + " option internal";
        final String displayNameForOption = "Checkbox option display";
        option.put(INTERNAL_NAME, internalNameForOption);
        option.put(DISPLAY_NAME, displayNameForOption);
        options.put(option);

        fieldCreationBody.put(TYPE, CHECKBOX.name());
        fieldCreationBody.put(OPTIONS, options);

        return fieldCreationBody;
    };

    private final Supplier<JSONObject> singleSelectDropdownType = () -> {
        final JSONObject fieldCreationBody = numberType.get();
        final JSONArray options = new JSONArray();

        for (int i = 0; i < 2; i++) {
            final JSONObject option = new JSONObject();
            final String internalNameForOption = getRandomInt() + " option internal";
            final String displayNameForOption = "Single select dropdown option display";

            option.put(INTERNAL_NAME, internalNameForOption);
            option.put(DISPLAY_NAME, displayNameForOption);
            options.put(option);
        }
        fieldCreationBody.put(TYPE, SINGLE_SELECT_DROPDOWN.name());
        fieldCreationBody.put(OPTIONS, options);

        return fieldCreationBody;
    };

    private final Supplier<JSONObject> multiSelectDropdownType = () -> {
        final JSONObject fieldCreationBody = numberType.get();
        final JSONArray options = new JSONArray();

        for (int i = 0; i < 2; i++) {
            final JSONObject option = new JSONObject();
            final String internalNameForOption = getRandomInt() + " option internal";
            final String displayNameForOption = "Multi select dropdown option display";

            option.put(INTERNAL_NAME, internalNameForOption);
            option.put(DISPLAY_NAME, displayNameForOption);
            options.put(option);
        }
        fieldCreationBody.put(TYPE, MULTI_SELECT_DROPDOWN.name());
        fieldCreationBody.put(OPTIONS, options);

        return fieldCreationBody;
    };

    private final Supplier<JSONObject> radiobuttonType = () -> {
        final JSONObject fieldCreationBody = numberType.get();
        final JSONArray options = new JSONArray();

        for (int i = 0; i < 2; i++) {
            final JSONObject option = new JSONObject();
            final String internalNameForOption = getRandomInt() + " option internal";
            final String displayNameForOption = "Multi select dropdown option display";

            option.put(INTERNAL_NAME, internalNameForOption);
            option.put(DISPLAY_NAME, displayNameForOption);
            options.put(option);
        }
        fieldCreationBody.put(TYPE, RADIOBUTTON.name());
        fieldCreationBody.put(OPTIONS, options);

        return fieldCreationBody;
    };

    {
        MAP.put(NUMBER, numberType);
        MAP.put(TEXT, textType);
        MAP.put(CHECKBOX, checkboxType);
        MAP.put(RADIOBUTTON, radiobuttonType);
        MAP.put(SINGLE_SELECT_DROPDOWN, singleSelectDropdownType);
        MAP.put(MULTI_SELECT_DROPDOWN, multiSelectDropdownType);
    }

    public JSONObject bodyBuilder(FieldTypes fieldType) {
        return MAP.get(fieldType).get();
    }
}
