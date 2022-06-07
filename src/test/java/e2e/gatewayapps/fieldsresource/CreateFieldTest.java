package e2e.gatewayapps.fieldsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.FieldsHelper;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.fieldsresource.payloads.FieldsCreationBody;
import helpers.flows.OrganizationFlows;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;
import static org.hamcrest.Matchers.*;


public class CreateFieldTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;
    private FieldsCreationBody fieldsCreationBody;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        fieldsCreationBody = new FieldsCreationBody();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @Xray(test = "PEG-1862", requirement = "PEG-1440")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void createFieldsWithValidUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        for (FieldTypes combination : FieldTypes.values()) {
            final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(combination);
            FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                    .then()
                    .statusCode(SC_CREATED)
                    .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"));
        }
    }

    @Xray(test = "PEG-1864", requirement = "PEG-1440")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void createFieldsByUnsupportedUsers(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");

        for (FieldTypes combination : FieldTypes.values()) {
            final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(combination);
            FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                    .then()
                    .statusCode(SC_FORBIDDEN)
                    .body("type", is("FORBIDDEN_ACCESS"));
        }
    }

    @Xray(test = "PEG-1865", requirement = "PEG-1440")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void createFieldWithoutInternalName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.NUMBER);
        fieldCreationBody.remove(FieldsCreationBody.INTERNAL_NAME);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1866", requirement = "PEG-1440")
    @Test
    public void createFieldWithoutNameTranslation() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.TEXT);
        fieldCreationBody.remove(FieldsCreationBody.DISPLAY_NAME);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

    }

    @Xray(test = "PEG-1867", requirement = "PEG-1440")
    @Test
    public void createFieldWithoutFieldType() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.TEXT);
        fieldCreationBody.remove(FieldsCreationBody.TYPE);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1868", requirement = "PEG-1440")
    @Test
    public void createFieldWithIncorrectFieldType() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.TEXT);
        fieldCreationBody.put(FieldsCreationBody.TYPE, "TYPE");
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(test = "PEG-1869", requirement = "PEG-1440")
    @Test
    public void createCheckboxFieldOptionMaximumNotExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.CHECKBOX);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 20; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " Name";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"));
    }

    @Xray(test = "PEG-1870", requirement = "PEG-1440")
    @Test
    public void createCheckboxFieldOptionMaximumExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.CHECKBOX);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 21; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " name";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1873", requirement = "PEG-1440")
    @Test
    public void createCheckboxFieldNoOptions() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.CHECKBOX);

        final JSONArray options = new JSONArray();
        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1871", requirement = "PEG-4677")
    @Test
    public void createSingleSelectDropdownFieldOptionMaximumNotExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.SINGLE_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 50; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " name";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
    }

    @Xray(test = "PEG-1872", requirement = "PEG-4677")
    @Test
    public void createSingleSelectDropdownFieldOptionMaximumExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.SINGLE_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 51; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " nameLabelKey";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1874", requirement = "PEG-4677")
    @Test
    public void createSingleSelectDropdownFieldOneOption() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.SINGLE_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        final JSONObject checkboxOption = new JSONObject();
        final String randomNameLabelKey = getRandomInt() + " name";
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        options.put(checkboxOption);

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5168", requirement = "PEG-4651")
    @Test
    public void createRadioButtonFieldOptionMaximumNotExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.RADIOBUTTON);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 20; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " name";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
    }

    @Xray(test = "PEG-5169", requirement = "PEG-4651")
    @Test
    public void createRadioButtonFieldOptionMaximumExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.RADIOBUTTON);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 21; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " nameLabelKey";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5170", requirement = "PEG-4651")
    @Test
    public void createRadioButtonFieldOneOption() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.RADIOBUTTON);

        final JSONArray options = new JSONArray();
        final JSONObject checkboxOption = new JSONObject();
        final String randomNameLabelKey = getRandomInt() + " name";
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        options.put(checkboxOption);

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5171", requirement = "PEG-4677")
    @Test
    public void createMultiSelectDropdownFieldOptionMaximumNotExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.MULTI_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 50; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " name";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
    }

    @Xray(test = "PEG-5172", requirement = "PEG-4677")
    @Test
    public void createMultiSelectDropdownFieldOptionMaximumExceeding() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.MULTI_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        for (int i = 0; i < 51; i++) {
            final JSONObject checkboxOption = new JSONObject();
            final String randomNameLabelKey = getRandomInt() + " nameLabelKey";
            checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
            checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
            options.put(checkboxOption);
        }

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5173", requirement = "PEG-4677")
    @Test
    public void createMultiSelectDropdownFieldOneOption() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.MULTI_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        final JSONObject checkboxOption = new JSONObject();
        final String randomNameLabelKey = getRandomInt() + " name";
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        options.put(checkboxOption);

        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-1875", requirement = "PEG-1440")
    @Test
    public void createFieldByOtherOrganizationUser() {
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");
        final Role role = getRandomOrganizationAdminRole();
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(getRandomEnumByClass(FieldTypes.class));
        FieldsHelper.createFields(token, otherOrganizationId, fieldCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-1876", requirement = "PEG-1440")
    @Test
    public void createFieldsSameNameAndSameType() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.TEXT);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(test = "PEG-5174", requirement = "PEG-1440")
    @Test
    public void createFieldsSameNameAndDifferentType() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject textFieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.TEXT);
        FieldsHelper.createFields(token, organizationId, textFieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
        final JSONObject radioButtonFieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.RADIOBUTTON);
        radioButtonFieldCreationBody.put(FieldsCreationBody.INTERNAL_NAME, textFieldCreationBody.getString(FieldsCreationBody.INTERNAL_NAME));
        FieldsHelper.createFields(token, organizationId, radioButtonFieldCreationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(test = "PEG-1877", requirement = "PEG-1440")
    @Test
    public void createFieldsSameInternalNameDifferentOrganizations() {
        final JSONObject organization1 = new OrganizationFlows().createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.TEXT);
        FieldsHelper.createFields(SUPPORT_TOKEN, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
        FieldsHelper.createFields(SUPPORT_TOKEN, organization1.getString("id"), fieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
    }

    @Xray(test = "PEG-1878", requirement = "PEG-4677")
    @Test
    public void createSingleSelectDropdownFieldSameOptions() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.SINGLE_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        final String randomNameLabelKey = getRandomInt() + " name";
        final JSONObject checkboxOption = new JSONObject();
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        for (int i = 0; i < 2; i++) {
            options.put(checkboxOption);
        }
        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(test = "PEG-5175", requirement = "PEG-4677")
    @Test
    public void createMultiSelectDropdownFieldSameOptions() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.MULTI_SELECT_DROPDOWN);

        final JSONArray options = new JSONArray();
        final String randomNameLabelKey = getRandomInt() + " name";
        final JSONObject checkboxOption = new JSONObject();
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        for (int i = 0; i < 2; i++) {
            options.put(checkboxOption);
        }
        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(test = "PEG-5176", requirement = "PEG-4651")
    @Test
    public void createRadioButtonFieldSameOptions() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.RADIOBUTTON);

        final JSONArray options = new JSONArray();
        final String randomNameLabelKey = getRandomInt() + " name";
        final JSONObject checkboxOption = new JSONObject();
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        for (int i = 0; i < 2; i++) {
            options.put(checkboxOption);
        }
        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(test = "PEG-5177", requirement = "PEG-1440")
    @Test
    public void createCheckboxFieldSameOptions() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.CHECKBOX);

        final JSONArray options = new JSONArray();
        final String randomNameLabelKey = getRandomInt() + " name";
        final JSONObject checkboxOption = new JSONObject();
        checkboxOption.put(FieldsCreationBody.INTERNAL_NAME, randomNameLabelKey);
        checkboxOption.put(FieldsCreationBody.DISPLAY_NAME, randomNameLabelKey);
        for (int i = 0; i < 2; i++) {
            options.put(checkboxOption);
        }
        fieldCreationBody.put(FieldsCreationBody.OPTIONS, options);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Xray(test = "PEG-1882", requirement = "PEG-1440")
    @Test
    public void createFieldsBySupportForDeletedOrganization() {
        final JSONObject userOrganization = new OrganizationFlows().createAndDeletePublishedOrganization();
        final JSONObject organization = userOrganization.getJSONObject("ORGANIZATION");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(getRandomEnumByClass(FieldTypes.class));
        FieldsHelper.createFields(SUPPORT_TOKEN, organization.getString("id"), fieldCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(test = "PEG-5178", requirement = "PEG-1440")
    @Test
    public void createFieldsByUserForDeletedOrganization() {
        final JSONObject userOrganization = new OrganizationFlows().createAndDeletePublishedOrganization();
        final JSONObject organization = userOrganization.getJSONObject("ORGANIZATION");
        final String token = userOrganization.getJSONObject(getRandomOrganizationAdminRole().name()).getString("token");

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(getRandomEnumByClass(FieldTypes.class));
        FieldsHelper.createFields(token, organization.getString("id"), fieldCreationBody)
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("type", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-5179", requirement = "PEG-4677")
    @Test
    public void createDifferentFieldsWithSameOptions() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject radioButtonFieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.RADIOBUTTON);
        final JSONObject multiDropdownFieldCreationBody = fieldsCreationBody.bodyBuilder(FieldTypes.MULTI_SELECT_DROPDOWN);
        final JSONArray options = multiDropdownFieldCreationBody.getJSONArray(FieldsCreationBody.OPTIONS);
        radioButtonFieldCreationBody.put(FieldsCreationBody.OPTIONS, options);

        FieldsHelper.createFields(token, organizationId, radioButtonFieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
        FieldsHelper.createFields(token, organizationId, multiDropdownFieldCreationBody)
                .then()
                .statusCode(SC_CREATED);
    }

    @Xray(test = "PEG-5180", requirement = "PEG-4677")
    @Test
    public void createNonTextFieldsWithRegex() {
        final FieldTypes field = FieldTypes.getRandomTypeWithoutRegex();
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String regex = "[ab]{4,6}c";

        final JSONObject fieldCreationBody = fieldsCreationBody.bodyBuilder(field);
        fieldCreationBody.put(FieldsCreationBody.REGEX, regex);
        FieldsHelper.createFields(token, organizationId, fieldCreationBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

}
