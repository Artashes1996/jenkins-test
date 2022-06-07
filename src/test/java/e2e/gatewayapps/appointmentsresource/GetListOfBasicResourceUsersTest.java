package e2e.gatewayapps.appointmentsresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.appointmentsresource.AppointmentsHelper;
import helpers.appsapi.appointmentsresource.payloads.GetListOfResourceUsersRequestBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static configuration.Role.*;
import static helpers.appsapi.appointmentsresource.payloads.GetListOfResourceUsersRequestBody.QUERY;
import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.FIRST_NAME;
import static helpers.appsapi.invitationresource.payloads.InvitationCreationBody.LAST_NAME;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestUtils.FAKER;
import static utils.commons.ToggleAction.*;

public class GetListOfBasicResourceUsersTest extends BaseTest {

    private String supportToken;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private ServiceFlows serviceFlows;
    private UserFlows userFlows;
    private ResourceFlows resourceFlows;
    private GetListOfResourceUsersRequestBody resourceUsersRequestBody;

    private JSONObject organizationWithUsers;
    private String organizationId;
    private String locationId;

    @BeforeClass
    public void setup() {
        supportToken = getToken(SUPPORT);
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();
        userFlows = new UserFlows();
        resourceFlows = new ResourceFlows();
        resourceUsersRequestBody = new GetListOfResourceUsersRequestBody();

        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationWithUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5451")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void seeOtherResourcesWithAllUsers(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, activeVisibleServiceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(1))
                .body("id[0]", is(resourceId))
                .body("userId[0]", is(nullValue()))
                .body("email[0]", is(nullValue()))
                .body("fullName[0]", is(resource.getString("internalName")));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5452")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void seeEmployeeResourcesWithAllUsers(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String newUserId = newUser.getString("id");
        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(newUserId);
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, activeVisibleServiceId, newUserId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(1))
                .body("id[0]", is(resourceId))
                .body("userId[0]", is(newUserId))
                .body("email[0]", is(newUser.getString("email")))
                .body("fullName[0]", is(newUser.getString("firstName") + " " + newUser.getString("lastName")));
    }

    // TODO: Check this test case
    @Xray(requirement = "PEG-4818", test = "PEG-5498")
    @Test(enabled = false, dataProviderClass = RoleDataProvider.class, dataProvider = "rolesWithLocation")
    public void seeResourcesOfOtherLocationByUsersWithLocations(Role role) {
        final String token = organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId));
        final String newUserId = newUser.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, newLocationId, activeVisibleServiceId, newUserId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, newLocationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5453")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void seeInactiveOtherResources(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject resource = resourceFlows.createInactiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, activeVisibleServiceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5454")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void seeInactiveEmployeeResources(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String newUserId = newUser.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, activeVisibleServiceId, newUserId);
        userFlows.inactivateUserById(organizationId, newUserId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5455")
    @Test
    public void seeNotAcceptedEmployeeResources() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject newUser = userFlows.inviteUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String newUserId = userFlows.getUserId(newUser.getString("email"), organizationId);
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, activeVisibleServiceId, newUserId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5456")
    @Test
    public void seeDeletedEmployeeResources() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String newUserId = newUser.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, activeVisibleServiceId, newUserId);
        userFlows.deleteUser(organizationId, newUserId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5457")
    @Test
    public void seeEmployeeResourcesNotLinkedToLocation() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), Collections.singletonList(locationId));
        final String newUserId = newUser.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, activeVisibleServiceId, newUserId);
        userFlows.linkUnlinkLocationToUser(organizationId, newUserId, locationId, UNLINK);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5459")
    @Test
    public void seeOtherResourcesNotLinkedToLocation() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");


        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, activeVisibleServiceId, resourceId);
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationId, resourceId, UNLINK);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5460")
    @Test
    public void seeOtherResourcesNotLinkedToService() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, activeVisibleServiceId, resourceId);
        resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, resourceId, activeVisibleServiceId, UNLINK);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5461")
    @Test
    public void seeEmployeeResourcesNotLinkedToService() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), Collections.singletonList(locationId));
        final String newUserId = newUser.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, activeVisibleServiceId, newUserId);
        userFlows.linkUnlinkUserToLocationService(organizationId, newUserId, locationId, activeVisibleServiceId, UNLINK);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5462")
    @Test
    public void seeResourcesLinkedToInactiveService() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String inactiveServiceId = serviceFlows.createInactiveService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, inactiveServiceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(inactiveServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5463")
    @Test
    public void seeResourcesLinkedToHiddenService() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String hiddenServiceId = serviceFlows.createHiddenService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, hiddenServiceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(hiddenServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(1))
                .body("id[0]", is(resourceId))
                .body("userId[0]", is(nullValue()))
                .body("email[0]", is(nullValue()))
                .body("fullName[0]", is(resource.getString("internalName")));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5464")
    @Test
    public void seeResourcesOfOtherOrganization() {
        final Role role = getRandomOrganizationRole();
        final String token = organizationWithUsers.getJSONObject(role.name()).getString("token");

        final String organizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, serviceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5465")
    @Test
    public void seeResourcesOfOtherOrganizationBySupport() {
        final String newOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(newOrganizationId).getString("id");

        final JSONObject resource = resourceFlows.createActiveResource(newOrganizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createService(newOrganizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(newOrganizationId, locationId, serviceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(supportToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5466")
    @Test
    public void seeResourcesOfPausedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createPausedOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, serviceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5467")
    @Test
    public void seeResourcesOfBlockedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, serviceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5468")
    @Test
    public void seeResourcesOfUnpublishedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createUnpublishedOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationAndUsers.getJSONObject(role.name()).getString("token");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createHiddenService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, serviceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5469")
    @Test
    public void seeResourcesOfDeletedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(organizationId).getString("id");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createHiddenService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, serviceId, resourceId);
        organizationFlows.deleteOrganization(organizationId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(supportToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4818", test = "PEG-5470")
    @Test
    public void seeResourcesOfInactiveLocation() {
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");

        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId));
        final String resourceId = resource.getString("id");
        final String activeVisibleServiceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, newLocationId, activeVisibleServiceId, resourceId);
        locationFlows.inactivateLocation(organizationId, newLocationId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(activeVisibleServiceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, newLocationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }


    @Xray(requirement = "PEG-4818", test = "PEG-5471")
    @Test
    public void seeManyResources() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final Role role = getRandomAdminRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        locationFlows.linkUnlinkServicesToLocation(organizationId, newLocationId, Collections.singletonList(serviceId), LINK);

        final List<String> allResourcesNames = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId));
            final String resourceId = resource.getString("id");
            resourceFlows.linkUnlinkServiceToResource(organizationId, newLocationId, resourceId, serviceId, LINK);
            allResourcesNames.add(resource.getString("internalName"));

            final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), Collections.singletonList(newLocationId));
            final String newUserId = newUser.getString("id");
            userFlows.linkUnlinkUserToLocationService(organizationId, newUserId, newLocationId, serviceId, LINK);
            allResourcesNames.add(newUser.getString("firstName") + " " + newUser.getString("lastName"));
        }

        Collections.sort(allResourcesNames);
        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, newLocationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(allResourcesNames.size()))
                .body("fullName", is(allResourcesNames));
    }

    @Xray(requirement = "PEG-5309", test = "PEG-5472")
    @Test
    public void seeResourcesWithEmptyRequestBody() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject searchBody = new JSONObject();
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(requirement = "PEG-5309", test = "PEG-5473")
    @Test
    public void seeResourcesWithIncorrectServiceId() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder("IncorrectServiceId");
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", is(empty()));
    }

    @Xray(requirement = "PEG-5309", test = "PEG-5474")
    @Test
    public void searchResourcesWithIncorrectQuery() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        final String resourceId = resource.getString("id");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");

        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, serviceId, resourceId);

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        searchBody.put(QUERY, "searchForResourceThatDoesNotExist");
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-5309", test = "PEG-5475")
    @Test
    public void searchOtherResources() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(serviceId), LINK);

        final List<String> allResourcesNames = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final JSONObject resource = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
            final String resourceId = resource.getString("id");
            resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, resourceId, serviceId, LINK);
            allResourcesNames.add(resource.getString("internalName"));
        }

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        searchBody.put(QUERY, allResourcesNames.get(0).substring(15));
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("userId[0]", is(nullValue()))
                .body("email[0]", is(nullValue()))
                .body("fullName", contains(allResourcesNames.get(0)));

        searchBody.put(QUERY, allResourcesNames.get(0));
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(1))
                .body("userId[0]", is(nullValue()))
                .body("email[0]", is(nullValue()))
                .body("fullName[0]", is(allResourcesNames.get(0)));

    }

    @Xray(requirement = "PEG-5309", test = "PEG-5476")
    @Test
    public void searchEmployeeResources() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(serviceId), LINK);

        final List<String> allResourcesNames = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), Collections.singletonList(locationId));
            final String newUserId = newUser.getString("id");
            userFlows.linkUnlinkUserToLocationService(organizationId, newUserId, locationId, serviceId, LINK);

            allResourcesNames.add(newUser.getString("firstName") + " " + newUser.getString("lastName"));
        }

        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        searchBody.put(QUERY, allResourcesNames.get(0).substring(5));
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("fullName", contains(allResourcesNames.get(0)));

        searchBody.put(QUERY, allResourcesNames.get(0));
        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(1))
                .body("fullName[0]", is(allResourcesNames.get(0)));
    }

    @Xray(requirement = "PEG-5309", test = "PEG-5806")
    @Test
    public void searchEmployeeResourcesAfterUpdatingUserName() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(serviceId), LINK);

        JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), Collections.singletonList(locationId));
        final String newUserId = newUser.getString("id");
        userFlows.linkUnlinkUserToLocationService(organizationId, newUserId, locationId, serviceId, LINK);
        newUser = userFlows.updateUserName(organizationId, newUser, FAKER.funnyName().name(), FAKER.animal().name());

        final String fullName = newUser.getString(FIRST_NAME) + " " + newUser.getString(LAST_NAME);
        final JSONObject searchBody = resourceUsersRequestBody.bodyBuilder(serviceId);
        searchBody.put(QUERY, fullName);

        AppointmentsHelper.getListOfBasicResourceUsers(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("fullName", contains(fullName));
    }

}
