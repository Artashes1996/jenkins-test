package e2e.ui.pages.resources;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.flows.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.ResourceLocationPage;
import pages.SignInPage;
import utils.Xray;
import utils.commons.ToggleAction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static configuration.Role.*;

public class ResourceLocationsPageTest extends BasePageTest {

    private List<JSONObject> allLocations;
    private List<JSONObject> linkedLocationsToResource;
    private List<JSONObject> notLinkedLocationsToResource;
    private JSONObject ownerWithLocations;
    private JSONObject adminWithLocations;
    private JSONArray locations;
    private List<JSONObject> locationsLinkedToLocationAdmin;
    private JSONObject locationAdminWithLocations;

    private JSONObject staffWithLocations;

    private String resourceId;
    private String organizationId;
    private List<String> locationInternalNames;

    private ResourceFlows resourceFlows;
    private LocationFlows locationFlows;
    private OrganizationFlows organizationFlows;
    private JSONObject organizationAndUsers;

    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        resourceFlows = new ResourceFlows();
        UserFlows userFlows = new UserFlows();
        locationInternalNames = new ArrayList<>();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithOwner();
        final JSONObject organization = organizationAndUsers.getJSONObject("ORGANIZATION");
        organizationId = organization.getString("id");
        locations = new LocationFlows().createLocations(organizationId, 5);
        allLocations = IntStream.range(0, locations.length()).mapToObj(locations::getJSONObject)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        IntStream.range(0, allLocations.size()).forEach(i -> {
            locationInternalNames.add(locations.getJSONObject(i).getString("internalName"));
        });
        final List<JSONObject> locationsLinkedToOwner = Stream.of(allLocations.get(0), allLocations.get(2))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        ownerWithLocations = userFlows.createUser(organizationId, OWNER, locationsLinkedToOwner.stream().map(location ->
                location.getString("id")).collect(Collectors.toList()));
        final List<JSONObject> locationsLinkedToAdmin = Stream.of(allLocations.get(0), allLocations.get(2))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        adminWithLocations = userFlows.createUser(organizationId, Role.ADMIN, locationsLinkedToAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));
        locationsLinkedToLocationAdmin = Stream.of(allLocations.get(0), allLocations.get(1), allLocations.get(4))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        locationAdminWithLocations = userFlows.createUser(organizationId, Role.LOCATION_ADMIN,
                locationsLinkedToLocationAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));
        staffWithLocations = userFlows.createUser(organizationId, Role.STAFF, locationsLinkedToLocationAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));
        linkedLocationsToResource = Stream.of(allLocations.get(2), allLocations.get(4))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        notLinkedLocationsToResource = Stream.of(allLocations.get(0), allLocations.get(1), allLocations.get(3))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        resourceId = resourceFlows.createActiveResource(organizationId, List.of(linkedLocationsToResource.get(0).getString("id"),
                linkedLocationsToResource.get(1).getString("id"))).getString("id");
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6158")
    @Test
    public void seeLocationsBySupport() {
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, resourceId, supportToken);
        resourceLocationPage.openPage();
        resourceLocationPage.checkLocationsInListByAdminRoles(linkedLocationsToResource, notLinkedLocationsToResource);
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6180")
    @Test(dataProvider = "organizationAdminRoles", dataProviderClass = RoleDataProvider.class)
    public void seeLocationsByAdminRoles(Role role) {
        final String token = role.equals(OWNER) ? ownerWithLocations.getString("token") :
                adminWithLocations.getString("token");
        adminWithLocations.getString("token");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, resourceId, token);
        resourceLocationPage.openPage();
        resourceLocationPage.checkLocationsInListByAdminRoles(linkedLocationsToResource, notLinkedLocationsToResource);
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6186")
    @Test
    public void seeLocationsByLocationAdmin() {
        final String locationAdminUserToken = locationAdminWithLocations.getString("token");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, resourceId, locationAdminUserToken);
        resourceLocationPage.openPage();
        resourceLocationPage.checkLocationsInListByLocationAdminRole(locationsLinkedToLocationAdmin,
                linkedLocationsToResource);
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6471")
    @Test
    public void seeLocationsByStaff() {
        final String staffUserToken = staffWithLocations.getString("token");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe,
                organizationId, resourceId, staffUserToken);
        resourceLocationPage.openPage();
        resourceLocationPage.checkLocationsInListByStaff(linkedLocationsToResource);
        resourceLocationPage.checkLocationLinkUnlinkColumnMissing();
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6202")
    @Test
    public void seeEmptyLocationPage() {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe,
                organizationId, newResourceId, staffWithLocations.getString("token"));
        resourceLocationPage.openPage();
        resourceLocationPage.checkEmptyLocationPage();
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6206")
    @Test
    public void sortByLocationNames() {
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe,
                organizationId, resourceId, ownerWithLocations.getString("token"));
        resourceLocationPage.openPage();
        resourceLocationPage.clickOnSortingIcon();
        List<JSONObject> newListAllLocations = new ArrayList<>(List.copyOf(allLocations));
        resourceLocationPage.checkIfLocationsAreSorted(newListAllLocations);
        resourceLocationPage.clickOnSortingIcon();
        Collections.reverse(newListAllLocations);
        resourceLocationPage.checkIfLocationsAreSorted(newListAllLocations);
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6207")
    @Test
    public void checkPagination() {
        final JSONObject newOrganizationWithFiftyLocations = organizationFlows.createUnpublishedOrganizationWithOwner();
        final String newOrganizationId = newOrganizationWithFiftyLocations.getJSONObject("ORGANIZATION").getString("id");
        locationFlows.createLocations(newOrganizationId, 51);
        final String newResourceId = resourceFlows.createActiveResource(newOrganizationId, null).getString("id");
        final String ownerToken = newOrganizationWithFiftyLocations.getJSONObject(OWNER.name()).getString("token");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, newOrganizationId, newResourceId, ownerToken);
        resourceLocationPage.openPage();
        resourceLocationPage.checkLocationCountOfUserInPaginationText(51);
    }

    @Xray(requirement = "PEG-3456", test = "PEG-6208")
    @Test
    public void checkSearchFunctionalityWithWrongSearch() {
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, resourceId, ownerWithLocations.getString("token"));
        resourceLocationPage.openPage();
        final String stringToSearch = UUID.randomUUID().toString();
        resourceLocationPage.searchForLocation(stringToSearch);
        resourceLocationPage.checkEmptySearchResult(stringToSearch);
    }


    @Xray(requirement = "PEG-3456", test = "PEG-6209")
    @Test
    public void checkSearchFunctionality() {
        final String ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, List.of(allLocations.get(0).getString("id"),
                allLocations.get(1).getString("id"))).getString("id");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, newResourceId, ownerToken);
        resourceLocationPage.openPage();
        resourceLocationPage.searchForLocation(allLocations.get(0).getString("internalName"));
        resourceLocationPage.checkSearchResult(0, allLocations.get(0));
    }

    @Xray(requirement = "PEG-3663", test = "PEG-6469")
    @Test
    public void linkLocationToResource() {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, List.of(allLocations.get(0).getString("id"),
                allLocations.get(2).getString("id"), allLocations.get(3).getString("id"))).getString("id");
        final String locationAdminToken = locationAdminWithLocations.getString("token");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, newResourceId, locationAdminToken);
        resourceLocationPage.openPage();
        resourceLocationPage.linkUnlinkLocationByIndex(3, ToggleAction.LINK);
        final List<JSONObject> locationsThatLinkedToResource = List.of(allLocations.get(0), allLocations.get(1), allLocations.get(2), allLocations.get(3));
        resourceLocationPage.changeTab(ResourceLocationPage.Tabs.SERVICES);
        resourceLocationPage.changeTab(ResourceLocationPage.Tabs.LOCATIONS);
        resourceLocationPage.checkLocationsInListByLocationAdminRole(locationsLinkedToLocationAdmin, locationsThatLinkedToResource);
    }

    @Xray(requirement = "PEG-3663", test = "PEG-6212")
    @Test
    public void unlinkLocationToResource() {
        final Role role = getRandomAdminRole();
        final String userToken = role.equals(OWNER) ? ownerWithLocations.getString("token") :
                (role.equals(ADMIN) ? adminWithLocations.getString("token") : supportToken);
        final String newResourceId = resourceFlows.createActiveResource(organizationId,
                List.of(allLocations.get(0).getString("id"), allLocations.get(1).getString("id"), allLocations.get(2).getString("id"))).getString("id");
        final ResourceLocationPage resourceLocationPage = new ResourceLocationPage(browserToUse, versionToBe, organizationId, newResourceId, userToken);
        resourceLocationPage.openPage();
        resourceLocationPage.linkUnlinkLocationByIndex(0, ToggleAction.UNLINK);
        resourceLocationPage.linkUnlinkLocationByIndex(1, ToggleAction.UNLINK);
        final List<JSONObject> locationsLinkedToNewResource = List.of(allLocations.get(2));
        final List<JSONObject> locationsNotLinkedToNewResource = List.of(allLocations.get(0), allLocations.get(1), allLocations.get(3), allLocations.get(4));
        resourceLocationPage.changeTab(ResourceLocationPage.Tabs.SERVICES);
        resourceLocationPage.changeTab(ResourceLocationPage.Tabs.LOCATIONS);
        resourceLocationPage.checkLocationsInListByAdminRoles(locationsLinkedToNewResource, locationsNotLinkedToNewResource);
    }

}




