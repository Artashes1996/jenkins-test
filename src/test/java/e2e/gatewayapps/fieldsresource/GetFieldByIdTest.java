package e2e.gatewayapps.fieldsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.fieldsresource.data.FieldsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.FieldsHelper;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.flows.FieldsFlows;
import helpers.flows.OrganizationFlows;
import org.json.JSONObject;

import org.testng.annotations.*;
import utils.Xray;

import static configuration.Role.*;
import static helpers.appsapi.fieldsresource.payloads.FieldsCreationBody.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;

public class GetFieldByIdTest extends BaseTest {

    private String organizationId;
    private OrganizationFlows organizationFlows;
    private JSONObject organizationWithUsers;
    private FieldsFlows fieldsFlows;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        fieldsFlows = new FieldsFlows();
        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @Xray(test = "PEG-2319", requirement = "PEG-5246")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "validFieldTypesWithOptions")
    public void getAllFieldsWithOptions(FieldTypes fieldType) {
        final JSONObject field = fieldsFlows.createField(organizationId, fieldType);
        FieldsHelper.getFieldById(SUPPORT_TOKEN, organizationId, field.getInt("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("organizationId", equalTo(organizationId))
                .body(DISPLAY_NAME, equalTo(field.getString(DISPLAY_NAME)))
                .body(INTERNAL_NAME, equalTo(field.getString(INTERNAL_NAME)))
                .body(OPTIONS, hasSize(field.getJSONArray(OPTIONS).length()));

    }

    @Xray(test = "PEG-2320", requirement = "PEG-5246")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "validFieldTypesWithoutOptions")
    public void getAllFieldsWithoutOptions(FieldTypes fieldType) {
        final JSONObject field = fieldsFlows.createField(organizationId, fieldType);
        FieldsHelper.getFieldById(SUPPORT_TOKEN, organizationId, field.getInt("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("organizationId", equalTo(organizationId))
                .body(DISPLAY_NAME, equalTo(field.getString(DISPLAY_NAME)))
                .body(INTERNAL_NAME, equalTo(field.getString(INTERNAL_NAME)));
    }

    // TODO Known issue - PEG-5897
    @Xray(test = "PEG-5904", requirement = "PEG-5246")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getFieldByValidUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject field = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        FieldsHelper.getFieldById(token, organizationId, field.getInt("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("organizationId", equalTo(organizationId))
                .body(DISPLAY_NAME, equalTo(field.getString(DISPLAY_NAME)))
                .body(INTERNAL_NAME, equalTo(field.getString(INTERNAL_NAME)));

    }

    @Xray(test = "PEG-5905", requirement = "PEG-5246")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void getDefaultField(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject defaultField = fieldsFlows.getDefaultFields(organizationId).getJSONObject(0);
        FieldsHelper.getFieldById(token, organizationId, defaultField.getInt("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("organizationId", equalTo(organizationId))
                .body(DISPLAY_NAME, equalTo(defaultField.getString(DISPLAY_NAME)))
                .body(INTERNAL_NAME, equalTo(defaultField.getString(INTERNAL_NAME)));
    }

    @Xray(test = "PEG-2322", requirement = "PEG-5246")
    @Test
    public void getFieldOfOtherOrganization() {
        final Role role = getRandomOrganizationRole();
        final String token = organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final int fieldId = fieldsFlows.createField(organizationId, FieldTypes.TEXT).getInt("id");
        FieldsHelper.getFieldById(token, otherOrganizationId, fieldId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));

    }

    @Xray(test = "PEG-2323", requirement = "PEG-5246")
    @Test
    public void getFieldByNonExistingFieldId() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final int nonExistingField = getRandomInt();
        FieldsHelper.getFieldById(token, organizationId, nonExistingField)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-2324", requirement = "PEG-5246")
    @Test
    public void getFieldWithNonExistingOrganizationId() {
        final int fieldId = fieldsFlows.createField(organizationId, FieldTypes.NUMBER).getInt("id");
        final String nonExistingOrganizationId = "orgId" + getRandomInt();
        FieldsHelper.getFieldById(SUPPORT_TOKEN, nonExistingOrganizationId, fieldId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5906", requirement = "PEG-5246")
    @Test
    public void getFieldOfDeletedOrganization() {
        final JSONObject organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final JSONObject field = fieldsFlows.createField(organizationId, FieldTypes.getRandomType());
        organizationFlows.deleteOrganization(organizationWithUsers.getJSONObject("ORGANIZATION").getString("id"));

        FieldsHelper.getFieldById(SUPPORT_TOKEN, organizationId, field.getInt("id"))
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/createAndUpdateField.json"))
                .body("organizationId", equalTo(organizationId))
                .body(DISPLAY_NAME, equalTo(field.getString(DISPLAY_NAME)))
                .body(INTERNAL_NAME, equalTo(field.getString(INTERNAL_NAME)));
    }

    @Xray(test = "PEG-5907", requirement = "PEG-5246")
    @Test
    public void getDeletedField() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final int fieldId = fieldsFlows.createField(organizationId, FieldTypes.getRandomType()).getInt("id");
        fieldsFlows.deleteField(organizationId, fieldId);

        FieldsHelper.getFieldById(token, organizationId, fieldId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(test = "PEG-5908", requirement = "PEG-5246")
    @Test
    public void getEditedField() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final String newName = FAKER.backToTheFuture().character();
        final FieldTypes fieldType = FieldTypes.getRandomType();
        final int fieldId = fieldsFlows.createField(organizationId, fieldType).getInt("id");
        fieldsFlows.editFieldNames(organizationId, fieldId, fieldType, newName);

        FieldsHelper.getFieldById(token, organizationId, fieldId)
                .then()
                .statusCode(SC_OK)
                .body(INTERNAL_NAME, is(newName))
                .body(DISPLAY_NAME, is(newName));
    }
}
