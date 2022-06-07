package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static configuration.Role.*;
import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.FAKER;
import static utils.TestUtils.getRandomInt;

public class GetServiceFieldsTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private UserFlows userFlows;
    private ServiceFlows serviceFlows;
    private FieldsFlows fieldsFlows;

    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;
    private String serviceId1;
    private JSONObject service1;
    private JSONObject service2;

    private List<JSONObject> fieldsToLink;
    private List<String> fieldsToLinkNames;
    private List<String> fieldsToLinkTypes;
    private List<String> fieldsToLinkDisplayTo;
    private List<Boolean> fieldsToLinkOptional;
    private List<String> namesDisplayedToEveryone;

    private JSONObject fieldLinkedToService2;
    private JSONObject fieldIndependent;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        serviceFlows = new ServiceFlows();
        fieldsFlows = new FieldsFlows();
        fieldsToLink = new ArrayList<>();

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        fieldIndependent = fieldsFlows.createField(organizationId, FieldTypes.NUMBER);

        service1 = serviceFlows.createService(organizationId);
        serviceId1 = service1.getString("id");
        for (FieldTypes fieldType : FieldTypes.values()) {
            final JSONObject field = new FieldsFlows().createField(organizationId, fieldType);
            field.put(DISPLAY_TO, Arrays.asList(ServiceUpdateRequestBody.DisplayTo.values()).get(getRandomInt(ServiceUpdateRequestBody.DisplayTo.values().length)).name());
            field.put(OPTIONAL, Math.random() < 0.5);
            fieldsToLink.add(field);
        }
        serviceFlows.addFieldsToService(organizationId, serviceId1, fieldsToLink);

        fieldsToLinkNames = fieldsToLink.stream().map(obj -> obj.getString("internalName")).collect(Collectors.toList());
        fieldsToLinkTypes = fieldsToLink.stream().map(obj -> obj.getString("type")).collect(Collectors.toList());
        fieldsToLinkDisplayTo = fieldsToLink.stream().map(obj -> obj.getString("displayTo")).collect(Collectors.toList());
        fieldsToLinkOptional = fieldsToLink.stream().map(obj -> obj.getBoolean("optional")).collect(Collectors.toList());

        namesDisplayedToEveryone = fieldsToLink.stream()
                .filter(obj -> obj.getString("displayTo").equals(DisplayTo.EVERYONE.name()))
                .map(obj -> obj.getString("internalName")).collect(Collectors.toList());

        fieldLinkedToService2 = new FieldsFlows().createField(organizationId, FieldTypes.CHECKBOX);
        fieldLinkedToService2.put(DISPLAY_TO, Arrays.asList(ServiceUpdateRequestBody.DisplayTo.values()).get(getRandomInt(ServiceUpdateRequestBody.DisplayTo.values().length)).name());
        fieldLinkedToService2.put(OPTIONAL, Math.random() < 0.5);
        service2 = serviceFlows.createServiceWithFields(organizationId, Collections.singletonList(fieldLinkedToService2));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6148")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getFieldsOfService(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        ServicesHelper.getFieldsLinkedToService(token, organizationId, serviceId1)
                .then()
                .statusCode(SC_OK)
                .body("name", is(fieldsToLinkNames))
                .body("type", is(fieldsToLinkTypes))
                .body("displayTo", is(fieldsToLinkDisplayTo))
                .body("optional", is(fieldsToLinkOptional));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6149")
    @Test
    public void getFieldsOfUnauthorizedUser() {
        ServicesHelper.getFieldsLinkedToService(null, organizationId, serviceId1)
                .then()
                .statusCode(SC_OK)
                .body("name", is(namesDisplayedToEveryone));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6150")
    @Test
    public void getFieldsByOtherOrganizationUser() {
        final String otherOrganizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final String otherOrganizationToken = userFlows.createUser(otherOrganizationId, getRandomOrganizationAdminRole(), null).getString("token");

        ServicesHelper.getFieldsLinkedToService(otherOrganizationToken, organizationId, serviceId1)
                .then()
                .statusCode(SC_OK)
                .body("name", is(namesDisplayedToEveryone));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6151")
    @Test
    public void getFieldsLinkedToServiceOfDeletedOrganization() {
        final String otherOrganizationToDelete = organizationFlows.createUnpublishedOrganization().getString("id");
        final JSONObject field = fieldsFlows.createField(otherOrganizationToDelete, FieldTypes.CHECKBOX);
        field.put(DISPLAY_TO, Arrays.asList(ServiceUpdateRequestBody.DisplayTo.values()).get(getRandomInt(ServiceUpdateRequestBody.DisplayTo.values().length)).name());
        field.put(OPTIONAL, Math.random() < 0.5);

        final String serviceId = serviceFlows.createServiceWithFields(otherOrganizationToDelete, Collections.singletonList(field)).getString("id");
        organizationFlows.deleteOrganization(otherOrganizationToDelete);

        ServicesHelper.getFieldsLinkedToService(SUPPORT_TOKEN, otherOrganizationToDelete, serviceId)
                .then()
                .statusCode(SC_OK)
                .body("name.size()", is(1))
                .body("name", hasItem(field.getString("internalName")));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6152")
    @Test
    public void getEditedFieldOfService() {
        final String newName = FAKER.funnyName().name();
        fieldsFlows.editFieldNames(organizationId, fieldLinkedToService2.getInt("id"), FieldTypes.CHECKBOX, newName);

        ServicesHelper.getFieldsLinkedToService(SUPPORT_TOKEN, organizationId, service2.getString("id"))
                .then()
                .statusCode(SC_OK)
                .body("name.size()", is(1))
                .body("name", hasItem(newName))
                .body("options.internalName[0]", contains(fieldLinkedToService2.getJSONArray("options").getJSONObject(0).getString("internalName")));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6153")
    @Test
    public void getServiceFieldsByInactiveUser() {
        final Role role = getRandomOrganizationRole();
        final JSONObject newUser = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.inactivateUserById(organizationId, newUser.getString("id"));

        ServicesHelper.getFieldsLinkedToService(newUser.getString("token"), organizationId, serviceId1)
                .then()
                .statusCode(SC_OK)
                .body("name", is(namesDisplayedToEveryone));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6154")
    @Test
    public void getServiceFieldsByDeletedUser() {
        final Role role = getRandomOrganizationRole();
        final JSONObject deletedUser = userFlows.createUser(organizationId, role, Collections.singletonList(locationId));
        userFlows.deleteUser(organizationId, deletedUser.getString("id"));

        ServicesHelper.getFieldsLinkedToService(deletedUser.getString("token"), organizationId, serviceId1)
                .then()
                .statusCode(SC_OK)
                .body("name", is(namesDisplayedToEveryone));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6155")
    @Test
    public void getServiceFieldsByInvalidOrganizationId() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String invalidOrganizationId = getRandomInt() + " org";
        ServicesHelper.getFieldsLinkedToService(token, invalidOrganizationId, serviceId1)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4873", test = "PEG-6156")
    @Test
    public void getServiceFieldsByInvalidServiceId() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String invalidServiceId = getRandomInt() + " service";

        ServicesHelper.getFieldsLinkedToService(token, organizationId, invalidServiceId)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }
}
