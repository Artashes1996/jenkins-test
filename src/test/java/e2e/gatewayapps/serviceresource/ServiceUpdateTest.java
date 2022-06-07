package e2e.gatewayapps.serviceresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody;
import helpers.flows.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static helpers.appsapi.servicesresource.payloads.ServiceUpdateRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;

import static configuration.Role.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class ServiceUpdateTest extends BaseTest {
    private String organizationId;
    private String ownerToken;
    private List<Integer> fieldsIds;
    private JSONObject service;

    @BeforeClass
    public void setUp() {
        final JSONObject organizationAndOwner = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndOwner.getJSONObject("ORGANIZATION").getString("id");
        final String ownerEmail = organizationAndOwner.getJSONObject(OWNER.name()).getString("email");
        final String password = "Qw!123456";
        ownerToken = new AuthenticationFlowHelper().getTokenWithEmailPassword(ownerEmail, password);
        fieldsIds = new ArrayList<>();
        service = new ServiceFlows().createService(organizationId);
        final int fieldTypeAmount = 5;
        final FieldsFlows fieldsFlows = new FieldsFlows();

        for (int i = 0; i < fieldTypeAmount; i++) {
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.CHECKBOX).getInt("id"));
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.TEXT).getInt("id"));
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.NUMBER).getInt("id"));
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.SINGLE_SELECT_DROPDOWN).getInt("id"));
        }
    }

    @Test(testName = "PEG-2056")
    public void createFieldLinks() {
        final int fieldTypeAmount = 4;
        final FieldsFlows fieldsFlows = new FieldsFlows();
        for (int i = 0; i < fieldTypeAmount; i++) {
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.CHECKBOX).getInt("id"));
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.TEXT).getInt("id"));
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.NUMBER).getInt("id"));
            fieldsIds.add(fieldsFlows.createField(organizationId, FieldTypes.SINGLE_SELECT_DROPDOWN).getInt("id"));
        }
        final JSONObject serviceUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.ALL_FIELDS);
        final JSONArray fieldLinkCreationRequest = new JSONArray();

        for (int i = 0; i < fieldsIds.size(); i++) {
            final int id = fieldsIds.get(i);
            boolean optional = i % 2 == 0;
            final JSONObject fieldLinkCreationRequestBody = new JSONObject();
            fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.DISPLAY_TO, ServiceUpdateRequestBody.DisplayTo.EVERYONE);
            fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.OPTIONAL, optional);
            fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.ORDER, i);
            fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.FIELD_ID, id);
            fieldLinkCreationRequest.put(fieldLinkCreationRequestBody);
        }
        serviceUpdateBody.put(ServiceUpdateRequestBody.FIELD_LINK_CREATION_REQUESTS, fieldLinkCreationRequest);

        ServicesHelper.updateService(ownerToken, organizationId, service.getString("id"), serviceUpdateBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"));
    }

    @Test(testName = "PEG-2052", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void updateServiceDisplayName(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final String serviceId = service.getString("id");
        final JSONObject updateRequestBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.REQUIRED);
        final String updatedServiceName = UUID.randomUUID().toString();
        updateRequestBody.put(ServiceUpdateRequestBody.NAME_TRANSLATION, updatedServiceName);
        ServicesHelper.updateService(token, organizationId, serviceId, updateRequestBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body(ServiceUpdateRequestBody.NAME_TRANSLATION, equalTo(updatedServiceName));

        ServicesHelper.updateService(token, organizationId, serviceId, updateRequestBody)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-2053", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void updateServiceDuration(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final String serviceId = service.getString("id");
        final JSONObject updateRequestBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.REQUIRED);
        final int updatedServiceDuration = new Random().nextInt(300) + 300;
        updateRequestBody.put(ServiceUpdateRequestBody.DURATION, updatedServiceDuration);
        ServicesHelper.updateService(token, organizationId, serviceId, updateRequestBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body(ServiceUpdateRequestBody.DURATION, equalTo(updatedServiceDuration));
    }

    @Test(testName = "PEG-2054", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void updateServiceInvalidDuration(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final String serviceId = service.getString("id");
        final JSONObject updateRequestBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.REQUIRED);
        final int updatedServiceDuration = new Random().nextInt(299);
        updateRequestBody.put(ServiceUpdateRequestBody.DURATION, updatedServiceDuration);
        ServicesHelper.updateService(token, organizationId, serviceId, updateRequestBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    // TODO enable test after fix
    @Test(enabled = false, testName = "PEG-2063", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 5)
    public void modifyFiledLinks(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final String serviceId = service.getString("id");
        final ServiceFlows serviceFlows = new ServiceFlows();

        final int fieldLinkIdToModify = serviceFlows.getFieldLinks(organizationId, serviceId).getJSONObject(0).getInt("id");
        final JSONObject modifyExistingFieldLink = new JSONObject();
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.DISPLAY_TO, ServiceUpdateRequestBody.DisplayTo.STAFF_ONLY);
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.ID, fieldLinkIdToModify);
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.OPTIONAL, false);
        final int order = 3;
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.ORDER, order);

        final JSONArray fieldLinksToModify = new JSONArray();
        fieldLinksToModify.put(modifyExistingFieldLink);

        final JSONObject modifyFieldLinks = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.ALL_FIELDS);
        modifyFieldLinks.put(ServiceUpdateRequestBody.FIELD_LINK_MODIFICATION_REQUEST, fieldLinksToModify);

        ServicesHelper.updateService(token, organizationId, service.getString("id"), modifyFieldLinks)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("fieldLinks.find{it.id=" + fieldLinkIdToModify + "}.order", equalTo(order))
                .body("fieldLinks.find{it.id=" + fieldLinkIdToModify + "}.displayTo", equalTo(ServiceUpdateRequestBody.DisplayTo.STAFF_ONLY.name()))
                .body("fieldLinks.find{it.id=" + fieldLinkIdToModify + "}.optional", equalTo(false));
    }

    @Test(testName = "PEG-2064", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 100)
    public void deleteAllFieldLinks(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final String serviceId = service.getString("id");
        final ServiceFlows serviceFlows = new ServiceFlows();
        final JSONObject updateRequestBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.ALL_FIELDS);
        final JSONArray fieldLinkIdToDelete = new JSONArray();
        final JSONArray fieldLinks = serviceFlows.getFieldLinks(organizationId, service.getString("id"));
        fieldLinks.forEach(fieldLink -> fieldLinkIdToDelete.put(((JSONObject) fieldLink).getInt("id")));
        updateRequestBody.put(ServiceUpdateRequestBody.FIELD_LINK_ID_TO_DELETE, fieldLinkIdToDelete);

        ServicesHelper.updateService(token, organizationId, serviceId, updateRequestBody)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body("fieldLinks", empty());
    }

    @Test(testName = "PEG-2057", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void createInvalidFieldLink(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONArray fieldLinkCreationRequest = new JSONArray();
        final JSONObject serviceUpdateBody = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.REQUIRED);
        final int id = new Random().nextInt();
        final JSONObject fieldLinkCreationRequestBody = new JSONObject();
        fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.DISPLAY_TO, ServiceUpdateRequestBody.DisplayTo.EVERYONE);
        fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.OPTIONAL, false);
        fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.ORDER, 0);
        fieldLinkCreationRequestBody.put(ServiceUpdateRequestBody.FIELD_ID, id);
        fieldLinkCreationRequest.put(fieldLinkCreationRequestBody);
        serviceUpdateBody.put(ServiceUpdateRequestBody.FIELD_LINK_CREATION_REQUESTS, fieldLinkCreationRequest);

        ServicesHelper.updateService(token, organizationId, service.getString("id"), serviceUpdateBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test(testName = "PEG-2059", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void modifyNonExistingFieldLink(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final int fieldLinkIdToModify = new Random().nextInt();
        final JSONObject modifyExistingFieldLink = new JSONObject();
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.DISPLAY_TO, ServiceUpdateRequestBody.DisplayTo.STAFF_ONLY);
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.ID, fieldLinkIdToModify);
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.OPTIONAL, false);
        final int order = 3;
        modifyExistingFieldLink.put(ServiceUpdateRequestBody.ORDER, order);

        final JSONArray fieldLinksToModify = new JSONArray();
        fieldLinksToModify.put(modifyExistingFieldLink);

        final JSONObject modifyFieldLinks = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.ALL_FIELDS);
        modifyFieldLinks.put(ServiceUpdateRequestBody.FIELD_LINK_MODIFICATION_REQUEST, fieldLinksToModify);

        ServicesHelper.updateService(token, organizationId, service.getString("id"), modifyFieldLinks)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Test(testName = "PEG-2060", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void changeResourceSelection(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject updateRequest = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.ALL_FIELDS);
        updateRequest.put(ServiceUpdateRequestBody.RESOURCE_SELECTION, ServiceUpdateRequestBody.ResourceSelection.REQUIRED);

        ServicesHelper.updateService(token, organizationId, service.getString("id"), updateRequest)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body(ServiceUpdateRequestBody.RESOURCE_SELECTION, equalTo(ServiceUpdateRequestBody.ResourceSelection.REQUIRED.name()));

        updateRequest.put(ServiceUpdateRequestBody.RESOURCE_SELECTION, ServiceUpdateRequestBody.ResourceSelection.DISABLED);

        ServicesHelper.updateService(token, organizationId, service.getString("id"), updateRequest)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(ServiceUpdateRequestBody.RESOURCE_SELECTION, equalTo(ServiceUpdateRequestBody.ResourceSelection.DISABLED.name()));
    }

    @Test(testName = "PEG-2061", dataProvider = "adminRoles", dataProviderClass = RoleDataProvider.class, priority = 10)
    public void changeServiceVisibility(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : ownerToken;
        final JSONObject updateRequest = new ServiceUpdateRequestBody().bodyBuilder(ServiceUpdateRequestBody.ServiceUpdateCombination.ALL_FIELDS);
        final Random random = new Random();
        final JSONObject visibility = new JSONObject();
        boolean monitorVisibility = random.nextBoolean();
        boolean physicalKioskVisibility = random.nextBoolean();
        boolean webKioskVisibility = random.nextBoolean();
        visibility.put(ServiceUpdateRequestBody.MONITOR, monitorVisibility);
        visibility.put(ServiceUpdateRequestBody.PHYSICAL_KIOSK, physicalKioskVisibility);
        visibility.put(WEB_KIOSK, webKioskVisibility);
        updateRequest.put(VISIBILITY, visibility);

        ServicesHelper.updateService(token, organizationId, service.getString("id"), updateRequest)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/createService.json"))
                .body(VISIBILITY + "." + WEB_KIOSK, equalTo(webKioskVisibility))
                .body(VISIBILITY + "." + MONITOR, equalTo(monitorVisibility))
                .body(VISIBILITY + "." + PHYSICAL_KIOSK, equalTo(physicalKioskVisibility));
    }

}
