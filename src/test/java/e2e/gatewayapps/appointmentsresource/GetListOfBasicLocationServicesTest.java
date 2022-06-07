package e2e.gatewayapps.appointmentsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.appointmentsresource.AppointmentsHelper;
import helpers.appsapi.locationservicelinksresource.LocationServiceLinksHelper;
import helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody;
import helpers.flows.*;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody.DESTINATION_GROUP_ID;
import static org.apache.hc.core5.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.commons.ToggleAction.*;

public class GetListOfBasicLocationServicesTest extends BaseTest {

    private String organizationId;
    private String locationId;
    private String serviceId;
    private String supportToken;
    private String internalName;
    private String resourceSelection;

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private ResourceFlows resourceFlows;
    private ServiceFlows serviceFlows;
    private UserFlows userFlows;
    private LocationServiceGroupFlows locationServiceGroupFlows;
    private JSONObject organizationAndUsersObject;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        resourceFlows = new ResourceFlows();
        serviceFlows = new ServiceFlows();
        userFlows = new UserFlows();
        locationServiceGroupFlows = new LocationServiceGroupFlows();

        organizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject serviceObject = serviceFlows.createService(organizationId);
        serviceId = serviceObject.getString("id");
        internalName = serviceObject.getString("internalName");
        resourceSelection = serviceObject.getString("resourceSelection");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, resourceId, Collections.singletonList(serviceId), LINK);
        supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
    }

    @Xray(test = "PEG-5265", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfBlockedOrganization() {
        final JSONObject blockedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : blockedOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String blockedOrganizationId = blockedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = locationFlows.createLocation(blockedOrganizationId).getString("id");
        final String newServiceId = serviceFlows.createService(blockedOrganizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(blockedOrganizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(blockedOrganizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.linkUnlinkServicesToResource(blockedOrganizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, blockedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK);
        organizationFlows.blockOrganization(blockedOrganizationId);
        AppointmentsHelper.getListOfLocationServices(userToken, blockedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5266", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfPausedOrganization() {
        final JSONObject pausedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : pausedOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String pausedOrganizationId = pausedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = locationFlows.createLocation(pausedOrganizationId).getString("id");
        final String newServiceId = serviceFlows.createService(pausedOrganizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(pausedOrganizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(pausedOrganizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.linkUnlinkServicesToResource(pausedOrganizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, pausedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK);
        organizationFlows.pauseOrganization(pausedOrganizationId);
        AppointmentsHelper.getListOfLocationServices(userToken, pausedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5356", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfUnPublishedOrganization() {
        final JSONObject unpublishedOrganizationAndUsersObject = organizationFlows.createUnpublishedOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : unpublishedOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String unpublishedOrganizationId = unpublishedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = locationFlows.createLocation(unpublishedOrganizationId).getString("id");
        final String newServiceId = serviceFlows.createService(unpublishedOrganizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(unpublishedOrganizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(unpublishedOrganizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.linkUnlinkServicesToResource(unpublishedOrganizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, unpublishedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5267", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfDeletedOrganizationBySupport() {
        final JSONObject deletedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String deletedOrganizationId = deletedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");
        final String newServiceId = serviceFlows.createService(deletedOrganizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(deletedOrganizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(deletedOrganizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.linkUnlinkServicesToResource(deletedOrganizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(supportToken, deletedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK);
        organizationFlows.deleteOrganization(deletedOrganizationId);
        AppointmentsHelper.getListOfLocationServices(supportToken, deletedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5340", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfDeletedOrganizationByUnSupportedRoles() {
        final JSONObject deletedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomOrganizationRole();
        final String userToken = deletedOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String deletedOrganizationId = deletedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String newLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");
        final String newServiceId = serviceFlows.createService(deletedOrganizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(deletedOrganizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(deletedOrganizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.linkUnlinkServicesToResource(deletedOrganizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, deletedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK);
        organizationFlows.deleteOrganization(deletedOrganizationId);
        AppointmentsHelper.getListOfLocationServices(userToken, deletedOrganizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_UNAUTHORIZED)
                .body("types[0]", is("UNAUTHORIZED_ACCESS"));
    }

    @Xray(test = "PEG-5268", requirement = "PEG-4779")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeListOfServicesInCaseOfInactiveLocation(Role role) {
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String inactiveLocationId = locationFlows.createInactiveLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(inactiveLocationId)).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(inactiveLocationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, inactiveLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, inactiveLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5271", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfInactiveService() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String inactiveServiceId = serviceFlows.createInactiveService(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(organizationId, inactiveServiceId, Collections.singletonList(newLocationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, newLocationId, newResourceId, Collections.singletonList(inactiveServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5272", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfInactiveResource() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String inactiveResourceId = resourceFlows.createInactiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.createActiveResource(organizationId, null);
        resourceFlows.linkUnlinkServicesToResource(organizationId, newLocationId, inactiveResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5273", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfNotHaveLinkedServiceToLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        serviceFlows.createService(organizationId).getString("id");
        resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5274", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfNotHaveLinkedResourceToLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.createActiveResource(organizationId, null).getString("id");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5275", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfNotHaveLinkedResourceToService() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5276", requirement = "PEG-4779")
    @Test
    public void seeListOfServicesInCaseOfHiddenService() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject serviceObject = serviceFlows.createHiddenService(organizationId);
        final String newServiceId = serviceObject.getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(newServiceId))
                .body("internalName[0]", is(serviceObject.getString("internalName")))
                .body("resourceSelection[0]", is(serviceObject.getString("resourceSelection")));
    }


    @Xray(requirement = "PEG-4779", test = "PEG-5279")
    @Test(dataProvider = "otherOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void seeListOfServicesByOtherOrganizationUsers(Role role) {
        final JSONObject otherOrganizationAndUsersObject = organizationFlows.createUnpublishedOrganizationWithAllUsers();
        final String otherOrganizationUserToken = otherOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        AppointmentsHelper.getListOfLocationServices(otherOrganizationUserToken, organizationId, locationId, new JSONObject())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-4779", test = "PEG-5280")
    @Test
    public void seeListOfServicesUsingFakeOrganizationIdBySupport() {
        final String fakeOrganizationId = UUID.randomUUID().toString();
        AppointmentsHelper.getListOfLocationServices(supportToken, fakeOrganizationId, locationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4779", test = "PEG-5281")
    @Test
    public void seeListOfServicesUsingFakeOrganizationIdByOrganizationRoles() {
        final String fakeOrganizationId = UUID.randomUUID().toString();
        final String userToken = organizationAndUsersObject.getJSONObject(Role.getRandomOrganizationRole().name()).getString("token");
        AppointmentsHelper.getListOfLocationServices(userToken, fakeOrganizationId, locationId, new JSONObject())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }


    @Xray(requirement = "PEG-4779", test = "PEG-5282")
    @Test
    public void seeListOfServicesUsingFakeLocationIdByOrganizationRoles() {
        final String fakeLocationId = UUID.randomUUID().toString();
        final String userToken = organizationAndUsersObject.getJSONObject(Role.getRandomOrganizationRole().name()).getString("token");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, fakeLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4779", test = "PEG-5307")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void seeListOfServices(Role role) {
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final List<String> newServiceIds = serviceFlows.createServices(organizationId, 4);
        locationFlows.linkUnlinkServicesToLocation(organizationId, newLocationId, newServiceIds, LINK);
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, newLocationId, newResourceId, newServiceIds, LINK);
        final ArrayList<String> serviceInternalNames = AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .extract().path("content.internalName");
        final ArrayList<String> copyServiceInternalNames = new ArrayList<>(serviceInternalNames);
        Collections.sort(serviceInternalNames);
        Assert.assertEquals(copyServiceInternalNames, serviceInternalNames);
    }

    @Xray(requirement = "PEG-4779", test = "PEG-5284")
    @Test
    public void seeListOfServicesInCaseOfServicesFromLocationServiceGroup() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        final String locationServiceGroupId = locationServiceGroupFlows.createGroup(organizationId, newLocationId, null).getString("id");
        final JSONObject locationServiceLinkBody = new LocationServiceLinksBody().bodyBuilder();
        locationServiceLinkBody.put(DESTINATION_GROUP_ID, locationServiceGroupId);
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        LocationServiceLinksHelper.linkServiceToGroup(supportToken, organizationId, newLocationId, newServiceId, locationServiceLinkBody);
        resourceFlows.linkUnlinkServicesToResource(organizationId, newLocationId, newResourceId, Collections.singletonList(newServiceId), LINK);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(newServiceId));
    }

    // TODO check this test
    @Xray(requirement = "PEG-4779", test = "PEG-5357")
    @Test(enabled = false)
    public void seeListOfServicesInCaseOfInvitedUserForEmployeeType() {
        final Role role = getRandomRole();
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String email = userFlows.inviteUser(organizationId, role, Collections.singletonList(newLocationId)).getString("email");
        final String newUserId = userFlows.getUserId(email, organizationId);
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        userFlows.linkUnlinkUserToLocationService(organizationId, newUserId, newLocationId, newServiceId, LINK);
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4779", test = "PEG-5359")
    @Test
    public void seeListOfServicesInCaseOfInactiveUserForEmployeeType() {
        final Role role = getRandomRole();
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, role, Collections.singletonList(newLocationId)).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(newLocationId));
        userFlows.linkUnlinkUserToLocationService(organizationId, newUserId, newLocationId, newServiceId, LINK);
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(newServiceId));
        userFlows.inactivateUserById(organizationId, newUserId);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, newLocationId, new JSONObject())
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5308", test = "PEG-5595")
    @Test
    public void searchServiceWithInvalidQuery() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        searchBody.put("query", "searchServiceWithDoesNotExist");
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5308", test = "PEG-5598")
    @Test
    public void searchServiceByServiceId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        searchBody.put("query", serviceId);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(serviceId))
                .body("internalName[0]", is(internalName))
                .body("resourceSelection[0]", is(resourceSelection));
    }

    @Xray(requirement = "PEG-5308", test = "PEG-5599")
    @Test
    public void searchServiceByServiceName() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        searchBody.put("query", internalName);
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(serviceId))
                .body("internalName[0]", is(internalName))
                .body("resourceSelection[0]", is(resourceSelection));
    }

    @Xray(requirement = "PEG-5308", test = "PEG-5612")
    @Test
    public void searchServiceByPartialServiceName() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        searchBody.put("query", internalName.substring(3, 9));
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(serviceId))
                .body("internalName[0]", is(internalName))
                .body("resourceSelection[0]", is(resourceSelection));
    }

    @Xray(requirement = "PEG-5308", test = "PEG-5599")
    @Test
    public void searchServiceByPartialServiceId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        searchBody.put("query", serviceId.substring(2, 8));
        AppointmentsHelper.getListOfLocationServices(userToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("id[0]", is(serviceId))
                .body("internalName[0]", is(internalName))
                .body("resourceSelection[0]", is(resourceSelection));
    }


}