package e2e.gatewayapps.fieldsresource;

import configuration.Role;
import e2e.gatewayapps.fieldsresource.data.FieldsDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.FieldsHelper;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class DeleteField {

    private OrganizationFlows organizationFlows;
    private FieldsFlows fieldsFlows;
    private UserFlows userFlows;

    private JSONObject organizationWithUsers;
    private String organizationId;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        fieldsFlows = new FieldsFlows();
        userFlows = new UserFlows();
        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5855")
    @Test(dataProviderClass = FieldsDataProvider.class, dataProvider = "validFieldTypes")
    public void deleteAllTypeOfFields(FieldTypes fieldType) {
        final int fieldId = fieldsFlows.createField(organizationId, fieldType).getInt("id");
        FieldsHelper.deleteField(SUPPORT_TOKEN, organizationId, fieldId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5856")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationAdminRoles")
    public void deleteFieldsByValidUsers(Role role) {
        final FieldTypes fieldType = FieldTypes.getRandomType();
        final String token = organizationWithUsers.getJSONObject(role.name()).getString("token");

        final int fieldId = fieldsFlows.createField(organizationId, fieldType).getInt("id");
        FieldsHelper.deleteField(token, organizationId, fieldId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5857")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "rolesWithLocation")
    public void deleteFieldsByInvalidUsers(Role role) {
        final FieldTypes fieldType = FieldTypes.getRandomType();
        final String token = organizationWithUsers.getJSONObject(role.name()).getString("token");

        final int fieldId = fieldsFlows.createField(organizationId, fieldType).getInt("id");
        FieldsHelper.deleteField(token, organizationId, fieldId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5858")
    @Test
    public void deleteFieldOfDeletedOrganization() {
        final String organizationToDelete = new OrganizationFlows().createBlockedOrganizationWithOwner()
                .getJSONObject("ORGANIZATION").getString("id");
        final FieldTypes fieldType = FieldTypes.getRandomType();

        final int fieldId = fieldsFlows.createField(organizationToDelete, fieldType).getInt("id");
        organizationFlows.deleteOrganization(organizationToDelete);

        FieldsHelper.deleteField(SUPPORT_TOKEN, organizationToDelete, fieldId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5859")
    @Test
    public void deleteFieldOfOtherOrganization() {
        final String otherOrganizationId = new OrganizationFlows().createUnpublishedOrganizationWithOwner()
                .getJSONObject("ORGANIZATION").getString("id");
        final Role role = getRandomAdminRole();
        final FieldTypes fieldType = FieldTypes.getRandomType();

        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : userFlows.createUser(organizationId, role, null).getString("token");
        final int otherOrganizationFieldId = fieldsFlows.createField(otherOrganizationId, fieldType).getInt("id");

        FieldsHelper.deleteField(token, organizationId, otherOrganizationFieldId)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5860")
    @Test
    public void deleteFieldByOtherOrganizationUser() {
        final String otherOrganizationId = new OrganizationFlows().createUnpublishedOrganizationWithOwner()
                .getJSONObject("ORGANIZATION").getString("id");
        final FieldTypes fieldType = FieldTypes.getRandomType();
        final Role role = getRandomOrganizationAdminRole();
        final String token = userFlows.createUser(organizationId, role, null).getString("token");

        final int otherOrganizationFieldId = fieldsFlows.createField(otherOrganizationId, fieldType).getInt("id");

        FieldsHelper.deleteField(token, otherOrganizationId, otherOrganizationFieldId)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-5246", test = "PEG-5861")
    @Test
    public void deleteDefaultField() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final int defaultFieldId = fieldsFlows.getDefaultFields(organizationId).getJSONObject(0).getInt("id");

        FieldsHelper.deleteField(token, organizationId, defaultFieldId)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("PRECONDITION_VIOLATED"));
    }

}
