package e2e.gatewayapps.locationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.LocationsHelper;
import helpers.appsapi.locationsresource.payloads.ToggleLinkServicesToLocationRequestBody;
import helpers.appsapi.servicesresource.ServicesHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.ServiceFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.*;
import static configuration.Role.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.*;

public class LinkUnlinkServicesToLocationTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private ServiceFlows serviceFlows;

    private JSONObject organizationAndUsers;
    private String organizationId;
    private String locationId;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();
        userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(test = "PEG-3688, PEG-3690", requirement = "PEG-2978")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkUnlinkServicesBySupportedRoles(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONArray servicesIds = new JSONArray();
        IntStream.range(0, 5).forEach(index -> servicesIds.put(serviceFlows.createService(organizationId).getString("id")));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);

        toggleLinkBody.put(ToggleLinkServicesToLocationRequestBody.ACTION, UNLINK);

        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3689, PEG-3691", requirement = "PEG-2978")
    @Test
    public void linkUnlinkServicesByUnsupportedRole() {
        final String token = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createService(organizationId).getString("id"));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_FORBIDDEN);

        toggleLinkBody.put(ToggleLinkServicesToLocationRequestBody.ACTION, UNLINK);

        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-3692", requirement = "PEG-2978")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void linkServiceToInactiveLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String inactiveLocationId = locationFlows.createInactiveLocation(organizationId).getString("id");
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createService(organizationId).getString("id"));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);
        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, inactiveLocationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3693", requirement = "PEG-2978")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void linkNonExistingServiceToLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createService(organizationId).getString("id"));
        servicesIds.put(UUID.randomUUID().toString());
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_NOT_FOUND);

        final String existingServiceForSearch = servicesIds.getString(0);

        ServicesHelper.searchForLinkedLocations(token, organizationId, existingServiceForSearch, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("content.linked", not(hasItem(true)));
    }

    @Xray(test = "PEG-3694", requirement = "PEG-2978")
    @Test(dataProvider = "extendedAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void linkServicesIncludingServiceFromOtherOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createService(organizationId).getString("id"));
        final String otherOrganizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        servicesIds.put(serviceFlows.createService(otherOrganizationId).getString("id"));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(token, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_NOT_FOUND);

        ServicesHelper.searchForLinkedLocations(token, organizationId, servicesIds.getString(0), new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("content.linked", not(hasItem(true)));
    }

    @Xray(test = "PEG-3696", requirement = "PEG-2978")
    @Test
    public void linkRequestWithoutAction() {
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createService(organizationId).getString("id"));
        JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);
        toggleLinkBody.remove(ToggleLinkServicesToLocationRequestBody.ACTION);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_BAD_REQUEST);

        toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, null);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-3697", requirement = "PEG-2978")
    @Test
    public void linkLinkedService() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final JSONArray serviceIds = new JSONArray();
        final String linkedServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceIds.put(linkedServiceId);
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, serviceIds);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);

        final String notLinkedServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceIds.put(notLinkedServiceId);
        toggleLinkBody.put(ToggleLinkServicesToLocationRequestBody.SERVICE_IDS, serviceIds);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("messages[0]", containsString(linkedServiceId))
                .body("messages[0]", not(containsString(notLinkedServiceId)));

        LocationsHelper.linkUnlinkServicesToLocation(locationAdminToken, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("messages[0]", containsString(linkedServiceId))
                .body("messages[0]", not(containsString(notLinkedServiceId)));
    }

    @Xray(test = "PEG-3698", requirement = "PEG-2978")
    @Test
    public void unlinkUnlinkedLocations() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final JSONArray serviceIds = new JSONArray();
        final String notLinkedServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceIds.put(notLinkedServiceId);
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(UNLINK, serviceIds);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_NOT_FOUND);

        LocationsHelper.linkUnlinkServicesToLocation(locationAdminToken, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-3699", requirement = "PEG-2978")
    @Test
    public void linkUnlinkToUnpublishedOrganization() {
        final String unpublishedOrganizationId = organizationFlows.createUnpublishedOrganization().getString("id");
        final String locationId = locationFlows.createLocation(unpublishedOrganizationId).getString("id");
        final String locationAdminToken = userFlows.createUser(unpublishedOrganizationId, LOCATION_ADMIN, Collections.singletonList(locationId))
                .getString("token");
        final JSONArray servicesIds = new JSONArray();
        IntStream.range(0, 5).forEach(index -> servicesIds.put(serviceFlows.createService(unpublishedOrganizationId).getString("id")));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, unpublishedOrganizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);

        toggleLinkBody.put(ToggleLinkServicesToLocationRequestBody.ACTION, UNLINK);

        LocationsHelper.linkUnlinkServicesToLocation(locationAdminToken, unpublishedOrganizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3700", requirement = "PEG-2978")
    @Test
    public void linkUnlinkToBlockedOrganization() {
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String blockedOrganizationId = blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = blockedOrganizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String locationAdminToken = blockedOrganizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final JSONArray servicesIds = new JSONArray();
        IntStream.range(0, 5).forEach(index -> servicesIds.put(serviceFlows.createService(blockedOrganizationId).getString("id")));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(locationAdminToken, blockedOrganizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);

        toggleLinkBody.put(ToggleLinkServicesToLocationRequestBody.ACTION, UNLINK);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, blockedOrganizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3701", requirement = "PEG-2978")
    @Test
    public void linkUnlinkToPausedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String pausedOrganizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = pausedOrganization.getJSONObject("LOCATION").getString("id");
        final String locationAdminToken = pausedOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final JSONArray servicesIds = new JSONArray();
        IntStream.range(0, 5).forEach(index -> servicesIds.put(serviceFlows.createService(pausedOrganizationId).getString("id")));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(locationAdminToken, pausedOrganizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);

        toggleLinkBody.put(ToggleLinkServicesToLocationRequestBody.ACTION, UNLINK);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, pausedOrganizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3702", requirement = "PEG-2978")
    @Test
    public void linkToNonExistingOrganization() {
        final JSONArray servicesIds = new JSONArray();
        IntStream.range(0, 5).forEach(index -> servicesIds.put(serviceFlows.createService(organizationId).getString("id")));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, UUID.randomUUID().toString(), locationId, toggleLinkBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-3703", requirement = "PEG-2978")
    @Test
    public void linkToNonExistingLocation() {
        final JSONArray servicesIds = new JSONArray();
        IntStream.range(0, 5).forEach(index -> servicesIds.put(serviceFlows.createService(organizationId).getString("id")));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);
        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, UUID.randomUUID().toString(), toggleLinkBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-3704", requirement = "PEG-2978")
    @Test
    public void linkToDeletedOrganizationLocation() {
        final JSONObject deletedOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String deletedOrganizationId = deletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");

        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createService(deletedOrganizationId).getString("id"));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);

        organizationFlows.deleteOrganization(deletedOrganizationId);

        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationLocationId, toggleLinkBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-3705", requirement = "PEG-2978")
    @Test
    public void linkInactiveService() {
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createInactiveService(organizationId).getString("id"));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);
        LocationsHelper.linkUnlinkServicesToLocation(SUPPORT_TOKEN, organizationId, locationId, toggleLinkBody)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-3714", requirement = "PEG-2978")
    @Test
    public void linkServiceByLocationAdminToAnotherLocation() {
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONArray servicesIds = new JSONArray();
        servicesIds.put(serviceFlows.createInactiveService(organizationId).getString("id"));
        final JSONObject toggleLinkBody = ToggleLinkServicesToLocationRequestBody.bodyBuilder(LINK, servicesIds);
        LocationsHelper.linkUnlinkServicesToLocation(locationAdminToken, organizationId, otherLocationId, toggleLinkBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

}