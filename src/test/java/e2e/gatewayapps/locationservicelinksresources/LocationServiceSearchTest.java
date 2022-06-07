package e2e.gatewayapps.locationservicelinksresources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationservicelinksresource.LocationServiceLinksHelper;
import helpers.flows.*;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import utils.Xray;
import utils.commons.ToggleAction;

import java.util.*;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.hc.core5.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;


public class LocationServiceSearchTest {

    private final String supportToken = new AuthenticationFlowHelper().getToken(SUPPORT);
    private LocationServiceGroupFlows groupFlows;
    private ServiceFlows serviceFlows;
    private LocationFlows locationFlows;

    private String organizationId;
    private JSONObject organizationWithUsers;
    private String locationId1;

    private JSONObject service1;
    private JSONObject group1;
    private JSONObject group1_1;
    private JSONObject group1_2;
    private JSONObject service1_1;
    private JSONObject service1_1_1;
    private JSONObject group2;
    private JSONObject group3;

    private List<String> rootLevelNames;
    private List<String> group1LevelNames;
    private List<String> group1_1LevelNames;
    private List<String> group3LevelNames;

    @BeforeClass
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        serviceFlows = new ServiceFlows();
        groupFlows = new LocationServiceGroupFlows();
        locationFlows = new LocationFlows();
        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");

        locationId1 = organizationWithUsers.getJSONObject("LOCATION").getString("id");
        service1 = serviceFlows.createService(organizationId);
        JSONObject service2 = serviceFlows.createService(organizationId);
        service1_1 = serviceFlows.createService(organizationId);
        JSONObject service1_2 = serviceFlows.createService(organizationId);
        service1_1_1 = serviceFlows.createService(organizationId);
        JSONObject service1_1_2 = serviceFlows.createService(organizationId);
        JSONObject service3_1 = serviceFlows.createService(organizationId);
        JSONObject service3_2 = serviceFlows.createService(organizationId);

        serviceFlows.linkLocationsToService(organizationId, service1.getString("id"), Collections.singletonList(locationId1));
        serviceFlows.linkLocationsToService(organizationId, service2.getString("id"), Collections.singletonList(locationId1));

        group1 = groupFlows.createGroup(organizationId, locationId1, null);
        serviceFlows.linkServiceToLocationGroup(organizationId, service1_1.getString("id"), locationId1, group1.getString("id"));
        serviceFlows.linkServiceToLocationGroup(organizationId, service1_2.getString("id"), locationId1, group1.getString("id"));

        group2 = groupFlows.createGroup(organizationId, locationId1, null);
        group3 = groupFlows.createGroup(organizationId, locationId1, null);
        serviceFlows.linkServiceToLocationGroup(organizationId, service3_1.getString("id"), locationId1, group3.getString("id"));
        serviceFlows.linkServiceToLocationGroup(organizationId, service3_2.getString("id"), locationId1, group3.getString("id"));

        group1_1 = groupFlows.createGroup(organizationId, locationId1, group1.getString("id"));
        group1_2 = groupFlows.createGroup(organizationId, locationId1, group1.getString("id"));
        serviceFlows.linkServiceToLocationGroup(organizationId, service1_1_1.getString("id"), locationId1, group1_1.getString("id"));
        serviceFlows.linkServiceToLocationGroup(organizationId, service1_1_2.getString("id"), locationId1, group1_1.getString("id"));

        rootLevelNames = new ArrayList<>();
        rootLevelNames.add(service1.getString("internalName"));
        rootLevelNames.add(service2.getString("internalName"));
        rootLevelNames.add(group1.getString("name"));
        rootLevelNames.add(group2.getString("name"));
        rootLevelNames.add(group3.getString("name"));

        group1LevelNames = new ArrayList<>();
        group1LevelNames.add(service1_1.getString("internalName"));
        group1LevelNames.add(service1_2.getString("internalName"));
        group1LevelNames.add(group1_1.getString("name"));
        group1LevelNames.add(group1_2.getString("name"));

        group1_1LevelNames = new ArrayList<>();
        group1_1LevelNames.add(service1_1_1.getString("internalName"));
        group1_1LevelNames.add(service1_1_2.getString("internalName"));

        group3LevelNames = new ArrayList<>();
        group3LevelNames.add(service3_1.getString("internalName"));
        group3LevelNames.add(service3_2.getString("internalName"));

    }

    @Xray(requirement = "PEG-2815", test = "PEG-4616")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void initialSortByName(Role role) {
        final String token = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final List<Map<String, Object>> children = LocationServiceLinksHelper.searchServicesInGroups(token, organizationId, locationId1)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/searchLocationService.json"))
                .extract().path("children");
        final List<String> namesInsideRoot = groupFlows.getNamesInsideGroup(children);
        Collections.sort(rootLevelNames);
        Assert.assertEquals(namesInsideRoot, rootLevelNames);

        final List<Map<String, Object>> insideGroup1 = groupFlows.getGroupsAndServicesInsideGroup(children, group1.getString("id"));
        final List<String> namesInsideGroup1 = groupFlows.getNamesInsideGroup(insideGroup1);
        Collections.sort(group1LevelNames);
        Assert.assertEquals(namesInsideGroup1, group1LevelNames);

        final List<Map<String, Object>> insideGroup2 = groupFlows.getGroupsAndServicesInsideGroup(children, group2.getString("id"));
        Assert.assertTrue(insideGroup2.isEmpty());

        final List<Map<String, Object>> insideGroup3 = groupFlows.getGroupsAndServicesInsideGroup(children, group3.getString("id"));
        final List<String> namesInsideGroup3 = groupFlows.getNamesInsideGroup(insideGroup3);
        Collections.sort(group3LevelNames);
        Assert.assertEquals(namesInsideGroup3, group3LevelNames);

        final List<Map<String, Object>> insideGroup1_1 = groupFlows.getGroupsAndServicesInsideGroup(insideGroup1, group1_1.getString("id"));
        final List<String> namesInsideGroup1_1 = groupFlows.getNamesInsideGroup(insideGroup1_1);
        Collections.sort(group1_1LevelNames);
        Assert.assertEquals(namesInsideGroup1_1, group1_1LevelNames);

        final List<Map<String, Object>> insideGroup1_2 = groupFlows.getGroupsAndServicesInsideGroup(insideGroup1, group1_2.getString("id"));
        Assert.assertTrue(insideGroup1_2.isEmpty());
    }

    @Xray(requirement = "PEG-2815", test = "PEG-4619")
    @Test
    public void searchServiceOfDeletedOrganization() {
        final Role role = Role.getRandomOrganizationRole();
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String userToken = organizationAndUsers.getJSONObject(role.name()).getString("token");
        new OrganizationFlows().deleteOrganization(organizationAndUsers.getJSONObject("ORGANIZATION").getString("id"));
        LocationServiceLinksHelper.searchServicesInGroups(supportToken, organizationId, locationId1)
                .then()
                .statusCode(SC_OK);
        LocationServiceLinksHelper.searchServicesInGroups(userToken, organizationId, locationId1)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Xray(requirement = "PEG-2815", test = "PEG-4618")
    @Test
    public void searchServiceByOtherOrganizationUser() {
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        LocationServiceLinksHelper.searchServicesInGroups(ownerToken, organizationId, locationId1)
                .then()
                .statusCode(SC_FORBIDDEN);
        LocationServiceLinksHelper.searchServicesInGroups(locationAdminToken, organizationId, locationId1)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(requirement = "PEG-5518", test = "PEG-6008")
    @Test
    public void getUpdatedServiceStatus() {
        final Role role = getRandomRole();
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String userToken = role.equals(SUPPORT) ? supportToken : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final List<String> servicesIds = serviceFlows.createServices(organizationId, 5);
        locationFlows.linkUnlinkServicesToLocation(organizationId, newLocationId, servicesIds, ToggleAction.LINK);
        final List<String> inactiveServiceIds = Arrays.asList(servicesIds.get(2), servicesIds.get(3));
        serviceFlows.inactivateServices(organizationId, inactiveServiceIds);
        LocationServiceLinksHelper.searchServicesInGroups(userToken, organizationId, newLocationId)
                .then()
                .statusCode(SC_OK)
                .body("children.findAll { it.status == 'ACTIVE' }.size()", is(3))
                .body("children.findAll { it.status == 'ACTIVE' }.id", hasItems(servicesIds.get(0), servicesIds.get(1), servicesIds.get(4)))
                .body("children.findAll { it.status == 'INACTIVE' }.size()", is(2))
                .body("children.findAll { it.status == 'INACTIVE' }.id", hasItems(servicesIds.get(2), servicesIds.get(3)));
    }
}
