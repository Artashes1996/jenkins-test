package helpers.flows;

import helpers.appsapi.fieldsresource.FieldsHelper;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.fieldsresource.payloads.FieldsCreationBody;
import helpers.appsapi.fieldsresource.payloads.FieldsSearchBody;
import helpers.appsapi.fieldsresource.payloads.FieldsEditBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class FieldsFlows {


    public JSONObject createField(String organizationId, FieldTypes combination) {
        final JSONObject fieldCreationBody = new FieldsCreationBody().bodyBuilder(combination);
        return new JSONObject(FieldsHelper.createFields(SUPPORT_TOKEN, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public JSONObject createFieldWithGivenOptionCount(Object organizationId, FieldTypes combination, int optionCount) {
        final JSONObject fieldCreationBody = new FieldsCreationBody().bodyBuilder(combination);
        final JSONArray options = new JSONArray();

        for (int i = 0; i < optionCount; i++) {
            final JSONObject option = new JSONObject();
            option.put(FieldsCreationBody.INTERNAL_NAME, getRandomInt() + " field internal");
            option.put(FieldsCreationBody.DISPLAY_NAME, getRandomInt() + " field display");
            options.put(option);
        }
        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        return new JSONObject(FieldsHelper.createFields(SUPPORT_TOKEN, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract().body().asString());
    }

    public List<JSONObject> createAllTypesOfFields(String organizationId) {
        final List<JSONObject> customServices = new ArrayList<>();

        customServices.add(createField(organizationId, FieldTypes.TEXT));
        customServices.add(createField(organizationId, FieldTypes.NUMBER));
        customServices.add(createField(organizationId, FieldTypes.MULTI_SELECT_DROPDOWN));
        customServices.add(createField(organizationId, FieldTypes.SINGLE_SELECT_DROPDOWN));
        customServices.add(createField(organizationId, FieldTypes.RADIOBUTTON));
        customServices.add(createField(organizationId, FieldTypes.CHECKBOX));
        return customServices;
    }

    public List<JSONObject> createFieldsWithOptionAndWithoutOption(String organizationId, FieldTypes fieldWithoutOption, FieldTypes fieldWithOption) {
        final List<JSONObject> customFields = new ArrayList<>();
        customFields.add(createField(organizationId, fieldWithoutOption));
        customFields.add(createField(organizationId, fieldWithOption));
        return customFields;
    }

    public JSONArray getDefaultFields(String organizationId) {
        final JSONObject fieldSearchBody = new FieldsSearchBody().bodyBuilder(FieldsSearchBody.FieldsSearchCombination.DEFAULT_FIELDS);

        return new JSONObject(FieldsHelper.searchFields(SUPPORT_TOKEN, organizationId, fieldSearchBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .body().asString())
                .getJSONArray("content");
    }

    public JSONArray getServiceDefaultFields(String organizationId) {
        final JSONObject fieldSearchBody = new FieldsSearchBody().bodyBuilder(FieldsSearchBody.FieldsSearchCombination.SERVICE_DEFAULT_FIELDS);
        return new JSONObject(FieldsHelper.searchFields(SUPPORT_TOKEN, organizationId, fieldSearchBody)
                .then()
                .statusCode(SC_OK)
                .extract()
                .body().asString())
                .getJSONArray("content");
    }

    public void deleteField(String organizationId, int fieldId) {
        FieldsHelper.deleteField(SUPPORT_TOKEN, organizationId, fieldId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    public void editFieldNames(String organizationId, int fieldId, FieldTypes fieldType, String newName) {
        final JSONObject editBody = new FieldsEditBody().bodyBuilder(fieldType);
        editBody.put(FieldsEditBody.INTERNAL_NAME, newName);
        editBody.put(FieldsEditBody.DISPLAY_NAME, newName);

        FieldsHelper.editField(SUPPORT_TOKEN, organizationId, fieldId, editBody)
                .then()
                .statusCode(SC_OK);
    }

}
