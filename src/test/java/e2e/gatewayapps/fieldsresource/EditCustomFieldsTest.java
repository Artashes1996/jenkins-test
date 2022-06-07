package e2e.gatewayapps.fieldsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.fieldsresource.data.FieldsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.FieldsHelper;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.fieldsresource.payloads.FieldsEditBody;
import helpers.flows.FieldsFlows;
import helpers.flows.OrganizationFlows;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import java.util.List;
import java.util.stream.*;

import static configuration.Role.*;
import static helpers.appsapi.fieldsresource.payloads.FieldTypes.*;
import static helpers.appsapi.fieldsresource.payloads.FieldsCreationBody.DISPLAY_NAME;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class EditCustomFieldsTest extends BaseTest {

    private JSONObject organizationAndAllUsers;
    private String organizationId;
    private FieldsFlows fieldsFlows;
    private FieldsEditBody fieldsEditBody;

    @BeforeClass
    public void setUp() {
        fieldsFlows = new FieldsFlows();
        fieldsEditBody = new FieldsEditBody();
        organizationAndAllUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndAllUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @Xray(requirement = "PEG-1732", test = "PEG-2242")
    @Test
    public void editFieldByUnsupportedRole() {
        final Role randomUnsupportedRole = getRandomRolesWithLocation();
        final String token = organizationAndAllUsers.getJSONObject(randomUnsupportedRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomType();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5693")
    @Test(dataProvider = "validFieldTypes", dataProviderClass = FieldsDataProvider.class)
    public void editDisplayName(FieldTypes fieldType) {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final JSONObject field = fieldsFlows.createField(organizationId, fieldType);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(fieldType);
        editBody.put(FieldsEditBody.INTERNAL_NAME, field.getString("internalName"));
        if (fieldType.equals(TEXT)) {
            editBody.put(FieldsEditBody.REGEX, field.getString("regex"));
        }

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("displayName", is(editBody.getString("displayName")));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-2245")
    @Test(dataProvider = "validFieldTypes", dataProviderClass = FieldsDataProvider.class)
    public void editInternalName(FieldTypes fieldType) {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final JSONObject field = fieldsFlows.createField(organizationId, fieldType);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(fieldType);
        editBody.put(FieldsEditBody.DISPLAY_NAME, field.getString(DISPLAY_NAME));
        if (fieldType.equals(TEXT)) {
            editBody.put(FieldsEditBody.REGEX, field.getString("regex"));
        }

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("internalName", is(editBody.getString(FieldsEditBody.INTERNAL_NAME)))
                .body("id", is(field.getInt("id")));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5694")
    @Test(dataProvider = "validRegex", dataProviderClass = FieldsDataProvider.class)
    public void editRegexTextField(String regex) {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final JSONObject field = fieldsFlows.createField(organizationId, TEXT);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(TEXT);
        editBody.put(FieldsEditBody.REGEX, regex);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"));
    }

    @Xray(requirement = "PEG-5186", test = "PEG-5695")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void editInternalNameToExistingInternalName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(role.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomType();
        final JSONObject field = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        final JSONObject field1 = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.put(FieldsEditBody.INTERNAL_NAME, field.getString("internalName"));

        FieldsHelper.editField(token, organizationId, field1.getInt("id"), editBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5696")
    @Test
    public void editDisplayNameToExistingDisplayName() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomType();
        final JSONObject field = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        final JSONObject field1 = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.put(FieldsEditBody.DISPLAY_NAME, field.getString(DISPLAY_NAME));

        FieldsHelper.editField(token, organizationId, field1.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body(DISPLAY_NAME, is(editBody.getString(FieldsEditBody.DISPLAY_NAME)));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5697")
    @Test
    public void addOptionToFieldsWithoutOption() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithoutOption();
        final JSONObject fieldWithOption = fieldsFlows.createField(organizationId, FieldTypes.RADIOBUTTON);
        final JSONObject fieldWithoutOption = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, fieldWithOption.getJSONArray("options"));

        FieldsHelper.editField(token, organizationId, fieldWithoutOption.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-5186", test = "PEG-5698")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void changeOptionInternalName(Role role) {
        final String newInternalName = "new Internal Name for field";
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(role.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        final JSONObject optionToModify = field.getJSONArray("options").getJSONObject(0);
        optionToModify.put(FieldsEditBody.OPTION_MODIFICATION_INTERNAL_NAME, newInternalName);

        final JSONArray optionsToModify = new JSONArray().put(optionToModify);
        editBody.put(FieldsEditBody.OPTIONS_TO_MODIFY, optionsToModify);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body("options.internalName[0]", is(newInternalName));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5700")
    @Test
    public void changeOptionByNonExistingId() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        final JSONObject optionToModify = field.getJSONArray("options").getJSONObject(0);
        optionToModify.put(FieldsEditBody.OPTION_MODIFICATION_ID, 123456789);

        final JSONArray optionsToModify = new JSONArray().put(optionToModify);
        editBody.put(FieldsEditBody.OPTIONS_TO_MODIFY, optionsToModify);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5701")
    @Test
    public void addOption() {
        final String internalNameNewOption = "internal name to add";
        final String displayNameNewOption = "display name to add";

        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        final JSONObject optionToAdd = new JSONObject();
        optionToAdd.put(FieldsEditBody.OPTION_CREATION_INTERNAL_NAME, internalNameNewOption);

        final JSONArray optionsToCreate = new JSONArray().put(optionToAdd);
        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, optionsToCreate);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body("options.internalName", hasItem(internalNameNewOption));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5702")
    @Test
    public void addOptionWithId() {
        final String internalNameNewOption = "internal name to add";

        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        final JSONObject optionToAdd = new JSONObject();
        optionToAdd.put(FieldsEditBody.OPTION_CREATION_INTERNAL_NAME, internalNameNewOption);
        optionToAdd.put(FieldsEditBody.OPTION_MODIFICATION_ID, field.getJSONArray("options").getJSONObject(0).getInt("id"));

        final JSONArray optionsToCreate = new JSONArray().put(optionToAdd);
        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, optionsToCreate);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body("internalName", is(editBody.getString("internalName")))
                .body("options.internalName", hasItem(internalNameNewOption));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5703")
    @Test
    public void addOptionWithExistingInternalName() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, field.getJSONArray("options"));

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5704")
    @Test
    public void addOptionMaximumAmount() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        final JSONArray optionsToCreate = new JSONArray();

        final int maximumAllowedCount = type.equals(FieldTypes.RADIOBUTTON) || type.equals(FieldTypes.CHECKBOX) ? 20 : 50;
        final int optionsCountToAdd = maximumAllowedCount - field.getJSONArray("options").length();
        for (int i = 0; i < optionsCountToAdd; i++) {
            final JSONObject optionToAdd = new JSONObject();
            optionToAdd.put(FieldsEditBody.OPTION_CREATION_INTERNAL_NAME, getRandomInt() + " internalName");
            optionsToCreate.put(optionToAdd);
        }

        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, optionsToCreate);
        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("options", hasSize(maximumAllowedCount))
        ;
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5705")
    @Test
    public void addOptionExceedingMaximumAmount() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        final JSONArray optionsToCreate = new JSONArray();

        final int maximumAllowedCount = type.equals(FieldTypes.RADIOBUTTON) || type.equals(FieldTypes.CHECKBOX) ? 20 : 50;
        final int optionsCountToAdd = maximumAllowedCount - field.getJSONArray("options").length() + 1;
        for (int i = 0; i < optionsCountToAdd; i++) {
            final JSONObject optionToAdd = new JSONObject();
            optionToAdd.put(FieldsEditBody.OPTION_CREATION_INTERNAL_NAME, getRandomInt() + " internalName");
            optionsToCreate.put(optionToAdd);
        }

        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, optionsToCreate);
        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5706")
    @Test
    public void addOptionWithoutInternalName() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        final JSONObject optionToAdd = new JSONObject();

        final JSONArray optionsToCreate = new JSONArray().put(optionToAdd);
        editBody.put(FieldsEditBody.OPTIONS_TO_CREATE, optionsToCreate);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-5186", test = "PEG-5709")
    @Test
    public void editOptionWithoutInternalName() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        final JSONArray optionToEdit = field.getJSONArray("options");
        optionToEdit.getJSONObject(0).remove(FieldsEditBody.OPTION_MODIFICATION_INTERNAL_NAME);
        editBody.put(FieldsEditBody.OPTIONS_TO_MODIFY, optionToEdit);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5710")
    @Test
    public void deleteOption() {
        final int initialOptionCount = 5;
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createFieldWithGivenOptionCount(organizationId, type, initialOptionCount);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        final int lastFieldId = field.getJSONArray("options").getJSONObject(initialOptionCount - 1).getInt("id");
        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE).put(lastFieldId);
        field.getJSONArray("options").remove(initialOptionCount - 1);

        final List<Integer> fieldOptionsNotDeleted = IntStream.range(0, field.getJSONArray("options").length())
                .mapToObj(i -> field.getJSONArray("options").getJSONObject(i).getInt("id"))
                .collect(Collectors.toList());
        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body("options.id", is(fieldOptionsNotDeleted));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5711")
    @Test
    public void deleteSeveralOptions() {
        final int initialOptionCount = 5;
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createFieldWithGivenOptionCount(organizationId, type, initialOptionCount);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        final int fieldOptionId1 = field.getJSONArray("options").getJSONObject(initialOptionCount - 4).getInt("id");
        final int fieldOptionId2 = field.getJSONArray("options").getJSONObject(initialOptionCount - 2).getInt("id");
        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE).put(fieldOptionId1).put(fieldOptionId2);
        field.getJSONArray("options").remove(initialOptionCount - 2);
        field.getJSONArray("options").remove(initialOptionCount - 4);

        final List<Integer> fieldOptionsNotDeleted = IntStream.range(0, field.getJSONArray("options").length())
                .mapToObj(i -> field.getJSONArray("options").getJSONObject(i).getInt("id"))
                .collect(Collectors.toList());
        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body("options.id", is(fieldOptionsNotDeleted));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5712")
    @Test
    public void deleteOptionsWithNonExistingIndex() {
        final int initialOptionCount = 5;
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createFieldWithGivenOptionCount(organizationId, type, initialOptionCount);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE)
                .put(initialOptionCount + 2)
                .put(initialOptionCount - 2);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_NOT_FOUND)
         .body("type", is("RESOURCE_NOT_FOUND"));
    }


    @Xray(requirement = "PEG-1732", test = "PEG-5808")
    @Test
    public void editAndDeleteSameOption() {
        final int initialOptionCount = 3;
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createFieldWithGivenOptionCount(organizationId, type, initialOptionCount);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        final String newInternalName = "newInternalName";
        final JSONObject optionToModify = field.getJSONArray("options").getJSONObject(0);
        optionToModify.put(FieldsEditBody.OPTION_MODIFICATION_INTERNAL_NAME, newInternalName);
        optionToModify.put(FieldsEditBody.OPTION_MODIFICATION_ID, optionToModify.getInt("id"));

        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE).put(optionToModify.getInt("id"));
        final JSONArray optionsToModify = new JSONArray().put(optionToModify);
        editBody.put(FieldsEditBody.OPTIONS_TO_MODIFY, optionsToModify);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5713")
    @Test
    public void deleteMinimumAllowed() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE).put(0).put(1);

        final int fieldOptionId = field.getJSONArray("options").getJSONObject(0).getInt("id");
        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE).put(fieldOptionId);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

    }

    @Xray(requirement = "PEG-1732", test = "PEG-5714")
    @Test
    public void deleteOptionsOfFieldsWithNoOption() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithoutOption();
        final JSONObject field = fieldsFlows.createField(organizationId, type);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.put(FieldsEditBody.OPTION_IDS_TO_DELETE, new JSONArray().put(0));

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5715")
    @Test
    public void addRegexToNonTextFields() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithoutRegex();
        final JSONObject field = fieldsFlows.createField(organizationId, type);

        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);
        editBody.put(FieldsEditBody.REGEX, "[ab]{4,6}c");

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5716")
    @Test
    public void editDefaultField() {
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final JSONObject editBody = fieldsEditBody.bodyBuilder(TEXT);

        final int defaultFieldId = Integer.parseInt(FieldsHelper.searchFields(SUPPORT_TOKEN, organizationId, new JSONObject())
                .then()
                .extract()
                .path("content.find{it.name='Email'}.id"));

        FieldsHelper.editField(token, organizationId, defaultFieldId, editBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-1732", test = "PEG-5823")
    @Test
    public void deleteAndCreateOption() {
        final int initialOptionCount = 5;
        final Role randomAdminRole = getRandomAdminRole();
        final String token = randomAdminRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndAllUsers.getJSONObject(randomAdminRole.name()).getString("token");
        final FieldTypes type = FieldTypes.getRandomTypeWithOption();
        final JSONObject field = fieldsFlows.createFieldWithGivenOptionCount(organizationId, type, initialOptionCount);
        final JSONObject editBody = fieldsEditBody.bodyBuilder(type);

        final String internalNameToDelete = field.getJSONArray("options").getJSONObject(initialOptionCount - 3).getString("internalName");
        final int idToDelete = field.getJSONArray("options").getJSONObject(initialOptionCount - 3).getInt("id");
        editBody.getJSONArray(FieldsEditBody.OPTION_IDS_TO_DELETE).put(idToDelete);
        field.getJSONArray("options").remove(initialOptionCount - 3);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK);

        final JSONObject optionToAdd = new JSONObject();
        optionToAdd.put(FieldsEditBody.OPTION_CREATION_INTERNAL_NAME, internalNameToDelete);
        editBody.getJSONArray(FieldsEditBody.OPTIONS_TO_CREATE).put(optionToAdd);
        editBody.remove(FieldsEditBody.OPTION_IDS_TO_DELETE);

        FieldsHelper.editField(token, organizationId, field.getInt("id"), editBody)
                .then()
                .statusCode(SC_OK)
                .body("options", hasSize(initialOptionCount))
                .body("options.id", not(hasItem(idToDelete)))
                .body("options.internalName", hasItem(internalNameToDelete));
    }

}
