package helpers.flows;

import configuration.Role;
import helpers.appsapi.support.organizationsresource.OrganizationsHelper;
import helpers.appsapi.support.organizationsresource.payloads.BlockUnblockOrganizationRequestBody;
import helpers.appsapi.support.organizationsresource.payloads.CreateOrganizationRequestBody;
import helpers.appsapi.support.organizationsresource.payloads.DeleteRestoreOrganizationRequestBody;
import io.restassured.response.*;
import lombok.SneakyThrows;
import org.json.*;
import utils.MatchingUtils;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.support.organizationsresource.payloads.BlockUnblockOrganizationRequestBody.BlockUnblockCombination.*;
import static org.apache.hc.core5.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.LINK;

public class OrganizationFlows {

    @SneakyThrows
    public void deleteOrganization(String  organizationId) {
        final JSONObject deleteOrganization = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.DELETE);

        OrganizationsHelper.deleteOrganization(SUPPORT_TOKEN, organizationId, deleteOrganization)
                .then()
                .statusCode(SC_NO_CONTENT);
        Thread.sleep(20000);
    }

    public void restoreOrganization(Object organizationId) {
        final JSONObject deleteOrganizationBody = DeleteRestoreOrganizationRequestBody.bodyBuilder(DeleteRestoreOrganizationRequestBody.DeleteRestoreCombination.RESTORE);

        OrganizationsHelper.restoreOrganization(SUPPORT_TOKEN, organizationId, deleteOrganizationBody);
    }

    public void uploadLogo(Object organizationId) {
        final String filePath = "src/test/resources/files/pics/charmander.png";
        final String contentType = "image/png";
        helpers.appsapi.organizationsresource.OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, contentType)
                .then()
                .statusCode(SC_CREATED);
    }

    public void uploadLogoWithFilePath(Object organizationId, String filePath) {
        final String contentType = "image/png";
        helpers.appsapi.organizationsresource.OrganizationsHelper.uploadOrganizationImage(SUPPORT_TOKEN, organizationId, filePath, contentType)
                .then()
                .statusCode(SC_CREATED);
    }

    public JSONObject blockOrganization(String organizationId) {
        final Response response = OrganizationsHelper.blockOrganization(SUPPORT_TOKEN, organizationId, BlockUnblockOrganizationRequestBody.bodyBuilder(BLOCK));
        response.then().statusCode(SC_OK);
        return new JSONObject(response.getBody().asString());
    }

    public JSONObject unblockOrganization(String organizationId) {
        final Response response = OrganizationsHelper.unblockOrganization(SUPPORT_TOKEN, organizationId, BlockUnblockOrganizationRequestBody.bodyBuilder(UNBLOCK));
        response.then().statusCode(SC_OK);
        return new JSONObject(response.getBody().asString());
    }

    public JSONObject publishOrganization(String organizationId) {
        final Response response = OrganizationsHelper.publishOrganization(SUPPORT_TOKEN, organizationId);
        response.then().statusCode(SC_OK);
        return new JSONObject(response.getBody().asString());
    }

    public JSONObject pauseOrganization(String organizationId) {
        final Response response = helpers.appsapi.organizationsresource.OrganizationsHelper.pauseOrganization(SUPPORT_TOKEN, organizationId);
        response.then().statusCode(SC_OK);
        return new JSONObject(response.getBody().asString());
    }

    public void unpauseOrganization(String organizationId) {
        final Response response = helpers.appsapi.organizationsresource.OrganizationsHelper.unpauseOrganization(SUPPORT_TOKEN, organizationId);
        new JSONObject(response.getBody().asString());
    }

    public JSONObject createAndPublishOrganizationWithAllUsers() {
        final JSONObject organizationAndUsers = createUnpublishedOrganizationWithAllUsers();
        final JSONObject publishedOrganization = publishOrganization(organizationAndUsers.getJSONObject("ORGANIZATION").getString("id"));
        organizationAndUsers.put("ORGANIZATION", publishedOrganization);

        return organizationAndUsers;
    }

    public JSONObject createPausedOrganizationWithAllUsers() {

        final JSONObject organizationWithAllUsers = createAndPublishOrganizationWithAllUsers();
        pauseOrganization(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"));

        return organizationWithAllUsers;
    }

    public JSONObject createPausedOrganizationWithOwner() {

        final JSONObject organizationWithAllUsers = createAndPublishOrganizationWithAllUsers();
        pauseOrganization(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"));

        return organizationWithAllUsers;
    }

    public JSONObject createBlockedOrganizationWithOwner() {

        final JSONObject organizationWithAllUsers = createAndPublishOrganizationWithOwner();
        blockOrganization(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"));
        return organizationWithAllUsers;
    }

    public JSONObject createBlockedOrganizationWithAllUsers() {

        final JSONObject organizationWithAllUsers = createAndPublishOrganizationWithAllUsers();
        blockOrganization(organizationWithAllUsers.getJSONObject("ORGANIZATION").getString("id"));
        return organizationWithAllUsers;
    }

    public JSONObject createUnpublishedOrganizationWithAllUsers() {

        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(CreateOrganizationRequestBody.OrganizationCreateCombination.ALL_FIELDS);
        final Response response = OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody);
        final JSONObject organizationResponse = new JSONObject(response
                .then()
                .statusCode(MatchingUtils.expectedStatusOnRequest(createOrganizationBody, response.asPrettyString(), SC_CREATED))
        .extract().body().asString());

        final String organizationId = organizationResponse.getString("id");
        final JSONObject locationResponse = new LocationFlows().createLocation(organizationId);
        final ArrayList<String> locationIds = new ArrayList<>();
        locationIds.add(locationResponse.getString("id"));
        final UserFlows userFlows = new UserFlows();
        final JSONObject userResponse = userFlows.createUser(organizationId, OWNER, locationIds);

        final JSONObject adminUser = userFlows.createUser(organizationId, ADMIN, locationIds);
        final JSONObject locationAdmin = userFlows.createUser(organizationId, LOCATION_ADMIN, locationIds);
        final JSONObject staff = userFlows.createUser(organizationId, STAFF, locationIds);
        final JSONObject usersOrganizationLocation = new JSONObject();

        usersOrganizationLocation.put(OWNER.name(), userResponse);
        usersOrganizationLocation.put(ADMIN.name(), adminUser);
        usersOrganizationLocation.put(LOCATION_ADMIN.name(), locationAdmin);
        usersOrganizationLocation.put(STAFF.name(), staff);

        usersOrganizationLocation.put("ORGANIZATION", organizationResponse);
        usersOrganizationLocation.put("LOCATION", locationResponse);

        return usersOrganizationLocation;
    }

    public JSONObject createAndPublishOrganizationWithOwner() {
        final JSONObject organizationAndOwner = createUnpublishedOrganizationWithOwner();
        final JSONObject publishedOrganization = publishOrganization(organizationAndOwner.getJSONObject("ORGANIZATION").getString("id"));
        organizationAndOwner.put("ORGANIZATION", publishedOrganization);
        organizationAndOwner.put(OWNER.name(), organizationAndOwner.getJSONObject(OWNER.name()));

        return organizationAndOwner;
    }

    public JSONObject createUnpublishedOrganizationWithOwner() {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(CreateOrganizationRequestBody.OrganizationCreateCombination.ALL_FIELDS);
        final JSONObject organizationResponse = new JSONObject(OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .body().asString());

        final JSONObject userResponse = new UserFlows().createUser(organizationResponse.getString("id"), OWNER, null);

        final JSONObject userOrganization = new JSONObject();
        userOrganization.put(OWNER.name(), userResponse);
        userOrganization.put("ORGANIZATION", organizationResponse);

        return userOrganization;
    }

    public JSONObject createUnpublishedOrganization() {
        final JSONObject createOrganizationBody = CreateOrganizationRequestBody.bodyBuilder(CreateOrganizationRequestBody.OrganizationCreateCombination.ALL_FIELDS);
        return new JSONObject(OrganizationsHelper.createOrganization(SUPPORT_TOKEN, createOrganizationBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .body().asString());
    }

    public JSONObject createAndDeletePublishedOrganization() {
        final JSONObject userOrganization = createAndPublishOrganizationWithAllUsers();
        final String organizationId = userOrganization.getJSONObject("ORGANIZATION").getString("id");
        deleteOrganization(organizationId);
        return userOrganization;
    }

    public JSONObject orgWithUserLinkedUnlinkedLocationServices(Role role) {
        final JSONObject orgWithOwnerLinkedUnlinkedLocationServices = createAndPublishOrganizationWithOwner();
        final String orgId = orgWithOwnerLinkedUnlinkedLocationServices.getJSONObject("ORGANIZATION").getString("id");
        final JSONArray locations = new LocationFlows().createLocations(orgId, 3);
        final String linkedLocationId = locations.getJSONObject(0).getString("id");

        final JSONObject user = new UserFlows().createUser(orgId, role, Collections.singletonList(linkedLocationId));

        final List<String> linkedServicesIds = new ServiceFlows().createServices(orgId, 5);
        final List<String> notLinkedServicesIds = new ServiceFlows().createServices(orgId, 2);
        new LocationFlows().linkUnlinkServicesToLocation(orgId, linkedLocationId, linkedServicesIds, LINK);
        final JSONObject linkedLocation = new JSONObject();
        linkedLocation.put("id", linkedLocationId);
        linkedLocation.put("locationServiceIds", linkedServicesIds);
        linkedLocation.put("notLinkedServiceIdsToLocation", notLinkedServicesIds);
        user.put("LINKED_LOCATION", linkedLocation);
        orgWithOwnerLinkedUnlinkedLocationServices.put("LOCATIONS", locations);
        orgWithOwnerLinkedUnlinkedLocationServices.put("USER", user);

        return orgWithOwnerLinkedUnlinkedLocationServices;
    }

    public JSONObject orgWithUserLinkedUnlinkedLocationServicesInGroup(Role role) {
        final JSONObject orgWithOwnerLinkedUnlinkedLocationServices = orgWithUserLinkedUnlinkedLocationServices(role);
        final String orgId = orgWithOwnerLinkedUnlinkedLocationServices.getJSONObject("ORGANIZATION").getString("id");
        final String linkedLocationId = orgWithOwnerLinkedUnlinkedLocationServices.getJSONObject("USER").getJSONObject("LINKED_LOCATION").getString("id");
        final LocationServiceGroupFlows locationServiceGroupFlows =  new LocationServiceGroupFlows();
        final ServiceFlows serviceFlows = new ServiceFlows();
        final JSONObject serviceGroup = new JSONObject();
        serviceGroup.put("groupId", locationServiceGroupFlows.createGroup(orgId,linkedLocationId,null).getString("id"));
        serviceGroup.put("serviceId", orgWithOwnerLinkedUnlinkedLocationServices.getJSONObject("USER").getJSONObject("LINKED_LOCATION").getJSONArray("locationServiceIds").get(0));
        serviceFlows.linkServiceToGroup(orgId,linkedLocationId,serviceGroup.getString("serviceId"),serviceGroup.getString("groupId"));
        orgWithOwnerLinkedUnlinkedLocationServices.getJSONObject("USER").getJSONObject("LINKED_LOCATION").put("SERVICE_GROUP", serviceGroup);
        return orgWithOwnerLinkedUnlinkedLocationServices;
    }
}
