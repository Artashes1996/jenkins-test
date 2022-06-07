package e2e.gatewayapps.locationservicelinksresources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationservicelinksresource.LocationServiceLinksHelper;
import helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;
import utils.Xray;

import java.util.Collections;

import static configuration.Role.*;
import static helpers.appsapi.locationservicelinksresource.payloads.LocationServiceLinksBody.DESTINATION_GROUP_ID;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class LocationServiceLinksTest {

    private static final Logger LOGGER = Logger.getLogger(LocationServiceLinksTest.class);
    private final ThreadLocal<String> serviceIdThread = new ThreadLocal<>();

    private String organizationId;
    private String locationId1;
    private String rootId;
    private String group1Id;
    private String group1_1Id;
    private String group1_2Id;
    private String group2Id;
    private String group2_1Id;

    private JSONObject organizationWithUsers;
    private LocationFlows locationFlows;
    private ServiceFlows serviceFlows;
    private LocationServiceGroupFlows groupFlows;

    private JSONObject otherActiveOrganizationJson;
    private String otherActiveOrganizationId;
    private String activeOrgLocationId;
    private String activeOrgGroupId;

    private JSONObject organizationToPause;
    private String organizationToPauseId;
    private String locationIdOfPausedOrg;
    private String groupIdOfPausedOrg;

    private JSONObject organizationToBlock;
    private String organizationToBlockId;
    private String locationIdOfBlockedOrg;
    private String groupIdOfBlockedOrg;

    private JSONObject organizationToDelete;
    private String organizationToDeleteId;
    private String locationIdOfDeletedOrg;
    private String groupIdOfDeletedOrg;
    private String serviceIdOfDeletedOrg;

    @BeforeClass
    public void setup() {

        final OrganizationFlows organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        serviceFlows = new ServiceFlows();
        groupFlows = new LocationServiceGroupFlows();

        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");

        locationId1 = organizationWithUsers.getJSONObject("LOCATION").getString("id");

        final JSONObject initialGroup = groupFlows.createGroup(organizationId, locationId1, null);
        rootId = initialGroup.getString("parentId");
        group1Id = initialGroup.getString("id");
        group1_1Id = groupFlows.createGroup(organizationId, locationId1, group1Id).getString("id");
        group1_2Id = groupFlows.createGroup(organizationId, locationId1, group1Id).getString("id");
        group2Id = groupFlows.createGroup(organizationId, locationId1, null).getString("id");
        group2_1Id = groupFlows.createGroup(organizationId, locationId1, group2Id).getString("id");

        LOGGER.trace("Other Active Organization");
        otherActiveOrganizationJson = organizationFlows.createAndPublishOrganizationWithAllUsers();
        otherActiveOrganizationId = otherActiveOrganizationJson.getJSONObject("ORGANIZATION").getString("id");
        activeOrgLocationId = otherActiveOrganizationJson.getJSONObject("LOCATION").getString("id");
        activeOrgGroupId = groupFlows.createGroup(otherActiveOrganizationId, activeOrgLocationId, null).getString("id");

        LOGGER.trace("Organization To PAUSE");
        organizationToPause = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationToPauseId = organizationToPause.getJSONObject("ORGANIZATION").getString("id");
        locationIdOfPausedOrg = organizationToPause.getJSONObject("LOCATION").getString("id");
        groupIdOfPausedOrg = groupFlows.createGroup(organizationToPauseId, locationIdOfPausedOrg, null).getString("id");
        organizationFlows.pauseOrganization(organizationToPauseId);

        LOGGER.trace("Organization To BLOCK");
        organizationToBlock = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationToBlockId = organizationToBlock.getJSONObject("ORGANIZATION").getString("id");
        locationIdOfBlockedOrg = organizationToBlock.getJSONObject("LOCATION").getString("id");
        groupIdOfBlockedOrg = groupFlows.createGroup(organizationToBlockId, locationIdOfBlockedOrg, null).getString("id");
        organizationFlows.blockOrganization(organizationToBlockId);

        LOGGER.trace("Organization To DELETE");
        organizationToDelete = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationToDeleteId = organizationToDelete.getJSONObject("ORGANIZATION").getString("id");
        locationIdOfDeletedOrg = organizationToDelete.getJSONObject("LOCATION").getString("id");
        groupIdOfDeletedOrg = groupFlows.createGroup(organizationToDeleteId, locationIdOfDeletedOrg, null).getString("id");
        serviceIdOfDeletedOrg = serviceFlows.createService(organizationToDeleteId).getString("id");
        serviceFlows.linkLocationsToService(organizationToDeleteId, serviceIdOfDeletedOrg, Collections.singletonList(locationIdOfDeletedOrg));
        organizationFlows.deleteOrganization(organizationToDeleteId);
    }

    @BeforeMethod
    public void dataPreparation() {
        serviceIdThread.set(serviceFlows.createService(organizationId).getString("id"));
        serviceFlows.linkLocationsToService(organizationId, serviceIdThread.get(), Collections.singletonList(locationId1));
    }

    @Xray(test = "PEG-3569", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceToGroup(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(group1Id));
    }

    @Xray(test = "PEG-3583", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceToSubGroup(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1_1Id);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(group1_1Id));
    }

    @Xray(test = "PEG-3565", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters") //3565
    public void moveServiceFromGroupToGroup(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);
        serviceFlows.linkServiceToGroup(organizationId, locationId1, serviceIdThread.get(), group1Id);

        groupToAttach.put(DESTINATION_GROUP_ID, group2Id);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(group2Id));
    }

    @Xray(test = "PEG-3568", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceFromSubgroupToSubgroup(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        serviceFlows.linkServiceToGroup(organizationId, locationId1, serviceIdThread.get(), group1_1Id);

        groupToAttach.put(DESTINATION_GROUP_ID, group1_2Id);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(group1_2Id));

        groupToAttach.put(DESTINATION_GROUP_ID, group2_1Id);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(group2_1Id));
    }

    @Xray(test = "PEG-3571", requirement = "PEG-2910")
    @Test
    public void moveServiceToGroupByStaff() {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);
        final String staffToken = organizationWithUsers.getJSONObject(STAFF.name()).getString("token");
        LocationServiceLinksHelper.linkServiceToGroup(staffToken, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-3566", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceFromGroupToRoot(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        serviceFlows.linkServiceToGroup(organizationId, locationId1, serviceIdThread.get(), group1Id);

        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(rootId));
    }

    @Xray(test = "PEG-3567", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceFromSubgroupToRoot(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder().put(DESTINATION_GROUP_ID, JSONObject.NULL);
        serviceFlows.linkServiceToGroup(organizationId, locationId1, serviceIdThread.get(), group1_1Id);

        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(rootId));
    }

    @Xray(test = "PEG-3560", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceFromOneOrganizationToAnother(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : otherActiveOrganizationJson.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, activeOrgGroupId);

        LocationServiceLinksHelper.linkServiceToGroup(token, otherActiveOrganizationId, activeOrgLocationId, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_NOT_FOUND).body("messages[0]", containsString("No LocationServiceLink can be found by given "));
    }

    @Xray(test = "PEG-3570", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationLevelInviters")
    public void moveServiceToGroupByOtherOrganizationUsers(Role role) {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);
        LocationServiceLinksHelper.linkServiceToGroup(otherActiveOrganizationJson.getJSONObject(role.name()).getString("token"), organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-3584", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void updateServiceGroupWithoutAttachingToLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final String unattachedServiceId = serviceFlows.createService(organizationId).getString("id");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();

        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, unattachedServiceId, groupToAttach)
                .then()
                .statusCode(SC_NOT_FOUND);
        groupToAttach.put(DESTINATION_GROUP_ID, JSONObject.NULL);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, unattachedServiceId, groupToAttach)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-3578", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceToGroupOfPausedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationToPause.getJSONObject(role.name()).getString("token");
        final String serviceIdOfPausedOrg = serviceFlows.createService(organizationToPauseId).getString("id");
        serviceFlows.linkLocationsToService(organizationToPauseId, serviceIdOfPausedOrg, Collections.singletonList(locationIdOfPausedOrg));

        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, groupIdOfPausedOrg);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationToPauseId, locationIdOfPausedOrg, serviceIdOfPausedOrg, groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(groupIdOfPausedOrg));
    }

    @Xray(test = "PEG-3572", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceToGroupOfBlockedOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationToBlock.getJSONObject(role.name()).getString("token");
        final String serviceIdOfBlockedOrg = serviceFlows.createService(organizationToBlockId).getString("id");
        serviceFlows.linkLocationsToService(organizationToBlockId, serviceIdOfBlockedOrg, Collections.singletonList(locationIdOfBlockedOrg));

        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, groupIdOfBlockedOrg);
        LocationServiceLinksHelper.linkServiceToGroup(token, organizationToBlockId, locationIdOfBlockedOrg, serviceIdOfBlockedOrg, groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(groupIdOfBlockedOrg));
    }

    @Xray(test = "PEG-3577", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "organizationLevelInviters")
    public void moveServiceToGroupOfDeletedOrganization(Role role) {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, groupIdOfDeletedOrg);
        LocationServiceLinksHelper.linkServiceToGroup(organizationToDelete.getJSONObject(role.name()).getString("token"), organizationToDeleteId, locationIdOfDeletedOrg, serviceIdOfDeletedOrg, groupToAttach)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(test = "PEG-3586", requirement = "PEG-2910")
    @Test
    public void moveServiceToGroupOfDeletedOrganizationBySupport() {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, groupIdOfDeletedOrg);
        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationToDeleteId, locationIdOfDeletedOrg, serviceIdOfDeletedOrg, groupToAttach)
                .then()
                .statusCode(SC_FORBIDDEN);

    }

    @Xray(test = "PEG-3582", requirement = "PEG-2910")
    @Test
    public void moveServiceToSameGroupTwice() {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);
        serviceFlows.linkServiceToGroup(organizationId, locationId1, serviceIdThread.get(), group1Id);

        LocationServiceLinksHelper.linkServiceToGroup(SUPPORT_TOKEN, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_OK)
                .body("locationServiceGroupId", equalTo(group1Id));
    }

    @Xray(test = "PEG-3581", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "extendedAdminRoles")
    public void moveServiceToOtherOrganizationLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);

        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, activeOrgLocationId, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("messages[0]", containsString("No LocationServiceLink can be found by given"));
    }

    @Xray(test = "PEG-3585", requirement = "PEG-2910")
    @Test
    public void moveServiceToOtherOrganizationLocationByLocationAdmin() {
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, group1Id);

        LocationServiceLinksHelper.linkServiceToGroup(organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token"), organizationId, activeOrgLocationId, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("messages[0]", containsString("Access is denied")); //No Location can be found by given
    }

    @Xray(test = "PEG-3580", requirement = "PEG-2910")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "inviters")
    public void moveServiceToOtherOrganizationGroup(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, activeOrgGroupId);

        LocationServiceLinksHelper.linkServiceToGroup(token, organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("messages[0]", containsString("No LocationServiceGroup can be found by given "));
    }

    @Xray(test = "PEG-3579", requirement = "PEG-2910")
    @Test
    public void moveServiceToLocationWithNoAccess() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject newLocationAdmin = new UserFlows().createUser(organizationId, LOCATION_ADMIN, Collections.singletonList(newLocationId));
        final String otherServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, otherServiceId, Collections.singletonList(newLocationId));
        final String newGroupId = groupFlows.createGroup(organizationId, newLocationId, null).getString("id");

        final JSONObject groupToAttach = new LocationServiceLinksBody().bodyBuilder();
        groupToAttach.put(DESTINATION_GROUP_ID, newGroupId);
        LocationServiceLinksHelper.linkServiceToGroup(newLocationAdmin.getString("token"), organizationId, locationId1, serviceIdThread.get(), groupToAttach)
                .then()
                .statusCode(SC_FORBIDDEN);
    }


}
