package e2e.ui.pages.users;

import configuration.Role;
import e2e.ui.pages.BasePageTest;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import pages.SignInPage;
import pages.UserLocationPage;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static configuration.Role.*;


public class UserLocationPageTest extends BasePageTest {

    private UserFlows userFlows;
    private String organizationId;
    private JSONObject ownerWithoutLocations;
    private List<JSONObject> allLocations;
    private int allLocationCount;

    private List<JSONObject> locationsLinkedToOwner;
    private JSONObject ownerWithLocations;

    private List<JSONObject> locationsLinkedToAdmin;
    private List<JSONObject> locationsNotLinkedToAdmin;
    private JSONObject adminWithLocations;

    private List<JSONObject> locationsLinkedToLocationAdmin;
    private List<JSONObject> locationsNotLinkedToLocationAdmin;
    private JSONObject locationAdminWithLocations;

    private List<JSONObject> locationsLinkedToStaff1;
    private List<JSONObject> locationsNotLinkedToStaff1;
    private JSONObject staff1WithLocations;

    private JSONObject staff2WithLocations;

    @BeforeClass
    public void setup() {
        userFlows = new UserFlows();
        JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithOwner();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        ownerWithoutLocations = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONArray locations = new LocationFlows().createLocations(organizationId, 4);

        allLocations = IntStream.range(0,locations.length()).mapToObj(locations::getJSONObject)
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        allLocationCount = allLocations.size();

        locationsLinkedToOwner = Stream.of(allLocations.get(0), allLocations.get(2))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());

        ownerWithLocations = userFlows.createUser(organizationId, OWNER, locationsLinkedToOwner.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        locationsLinkedToAdmin = Stream.of(allLocations.get(0), allLocations.get(2))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        locationsNotLinkedToAdmin = Stream.of(allLocations.get(1), allLocations.get(3))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        adminWithLocations = userFlows.createUser(organizationId, Role.ADMIN, locationsLinkedToAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        locationsLinkedToLocationAdmin = Stream.of(allLocations.get(0), allLocations.get(1))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        locationsNotLinkedToLocationAdmin = Stream.of(allLocations.get(2), allLocations.get(3))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        locationAdminWithLocations = userFlows.createUser(organizationId, Role.LOCATION_ADMIN, locationsLinkedToLocationAdmin.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        locationsLinkedToStaff1 = Collections.singletonList(allLocations.get(0));
        locationsNotLinkedToStaff1 = Stream.of(allLocations.get(1), allLocations.get(2), allLocations.get(3))
                .sorted(Comparator.comparing((JSONObject obj) -> obj.getString("internalName")))
                .collect(Collectors.toList());
        staff1WithLocations = userFlows.createUser(organizationId, Role.STAFF, locationsLinkedToStaff1.stream().map(location -> location.getString("id")).collect(Collectors.toList()));

        final List<JSONObject> locationsLinkedToStaff2 = Collections.singletonList(allLocations.get(3));
        staff2WithLocations = userFlows.createUser(organizationId, Role.STAFF, locationsLinkedToStaff2.stream().map(location -> location.getString("id")).collect(Collectors.toList()));
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5124")
    @Test
    public void seeLocationsBySupport() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        userLocationPage.userToEnter.set(staff1WithLocations);
        userLocationPage.openPage();

        userLocationPage.checkLocationsInListByAdminRoles(locationsLinkedToStaff1, locationsNotLinkedToStaff1);
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5125")
    @Test
    public void seeAllLocationsByOwner() {
        final String token = Role.getRandomOrganizationAdminRole().equals(OWNER)?ownerWithLocations.getString("token"):adminWithLocations.getString("token");
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, token);
        userLocationPage.userToEnter.set(locationAdminWithLocations);
        userLocationPage.openPage();

        userLocationPage.checkLocationsInListByAdminRoles(locationsLinkedToLocationAdmin, locationsNotLinkedToLocationAdmin);
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5126")
    @Test
    public void seeLocationsByLocationAdmin() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, locationAdminWithLocations.getString("token"));
        userLocationPage.userToEnter.set(ownerWithLocations);
        userLocationPage.openPage();

        userLocationPage.checkLocationsInListByLocationAdminRole(locationsLinkedToLocationAdmin, locationsLinkedToOwner);
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5127")
    @Test
    public void seeLocationsByStaff() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, staff1WithLocations.getString("token"));
        userLocationPage.userToEnter.set(adminWithLocations);
        userLocationPage.openPage();

        userLocationPage.checkLocationsInListByStaff(locationsLinkedToAdmin);
        userLocationPage.checkLocationLinkUnlinkColumnMissing();
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5128")
    @Test(enabled = false) // TODO issue number - 2430
    public void seeEmptyLocationPageByStaff() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, staff2WithLocations.getString("token"));
        userLocationPage.userToEnter.set(ownerWithoutLocations);
        userLocationPage.openPage();

        userLocationPage.checkEmptyLocationPage();
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5129")
    @Test
    public void sortByName() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, ownerWithoutLocations.getString("token"));
        userLocationPage.userToEnter.set(adminWithLocations);
        userLocationPage.openPage();

        userLocationPage.orderByName();
        userLocationPage.checkLocationsInPage(allLocations);
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5130")
    @Test
    public void checkPagination() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, adminWithLocations.getString("token"));
        userLocationPage.userToEnter.set(ownerWithoutLocations);
        userLocationPage.openPage();

        userLocationPage.checkLocationCountOfUserInPaginationText(allLocationCount);
    }

    @Xray(requirement = "PEG-2430", test = "PEG-5131")
    @Test
    public void checkSearchFunctionality() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        userLocationPage.userToEnter.set(ownerWithoutLocations);
        userLocationPage.openPage();

        userLocationPage.searchForLocation(locationsNotLinkedToAdmin.get(0).getString("internalName"));
        userLocationPage.checkLocationsInPage(Collections.singletonList(locationsNotLinkedToAdmin.get(0)));
    }

    @Xray(requirement = "PEG-2430", test = "PEG-5132")
    @Test(enabled = false)  // TODO issue number - 2430
    public void checkSearchFunctionalityWithWrongOrNoSearch() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, organizationId, supportToken);
        userLocationPage.userToEnter.set(ownerWithoutLocations);
        userLocationPage.openPage();

        final String stringToSearch = UUID.randomUUID().toString();
        userLocationPage.searchForLocation(stringToSearch);
        userLocationPage.checkEmptySearchResult(stringToSearch);

        userLocationPage.searchForLocation("");
        final int expectedCount = locationsNotLinkedToAdmin.size() + locationsLinkedToAdmin.size();
        userLocationPage.checkLocationCountOfUserInPaginationText(expectedCount);
    }

    @Xray(requirement = "PEG-3006", test = "PEG-5133")
    @Test
    public void checkUserInfo() {
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, adminWithLocations.getString("token"));
        userLocationPage.userToEnter.set(locationAdminWithLocations);

        userLocationPage.openPage();
        userLocationPage.checkUserInfo(locationAdminWithLocations);
    }

    @Xray(requirement = "PEG-3287", test = "PEG-5134")
    @Test
    public void linkFirstLocationToUserAndCheckOrder() {
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationAdminRole(), null);
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, adminWithLocations.getString("token"));
        userLocationPage.userToEnter.set(newUser);
        userLocationPage.openPage();
        userLocationPage.linkUnlinkLocationByIndex(3);

        final List<JSONObject> locationsLinkedToNewUser = Collections.singletonList(allLocations.get(3));
        final List<JSONObject> locationsNotLinkedToNewUser = Arrays.asList(allLocations.get(0), allLocations.get(1), allLocations.get(2));

        userLocationPage.checkLinkedLocationDetailsByIndex(3, allLocations.get(3));
        userLocationPage.changeTab(UserLocationPage.Tabs.PERSONAL_DETAILS);
        userLocationPage.changeTab(UserLocationPage.Tabs.LOCATIONS);
        userLocationPage.checkLocationsInListByAdminRoles(locationsLinkedToNewUser, locationsNotLinkedToNewUser);
    }

    @Xray(requirement = "PEG-3287", test = "PEG-5135")
    @Test
    public void unlinkLastLocationToUserAndCheckOrder() {
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomRolesWithLocation(), allLocations.stream().map(location->location.getString("id")).collect(Collectors.toList()));
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, ownerWithLocations.getString("token"));
        userLocationPage.userToEnter.set(newUser);
        userLocationPage.openPage();
        userLocationPage.linkUnlinkLocationByIndex(0);

        List<JSONObject> locationsNotLinkedToNewUser = Collections.singletonList(allLocations.get(0));
        List<JSONObject> locationsLinkedToNewUser = Arrays.asList(allLocations.get(1), allLocations.get(2), allLocations.get(3));
        userLocationPage.checkUnlinkedLocationDetailsByIndex(0, allLocations.get(0));
        userLocationPage.changeTab(UserLocationPage.Tabs.PERSONAL_DETAILS);
        userLocationPage.changeTab(UserLocationPage.Tabs.LOCATIONS);
        userLocationPage.checkLocationsInListByAdminRoles(locationsLinkedToNewUser, locationsNotLinkedToNewUser);
    }

    @Xray(requirement = "PEG-3287", test = "PEG-5136")
    @Test(enabled = false) // TODO issue 5122
    public void unlinkLastLocationOfStaffOrLocationAdmin() {
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomRolesWithLocation(), Collections.singletonList(allLocations.get(0).getString("id")));
        final UserLocationPage userLocationPage = new UserLocationPage(browserToUse, versionToBe, ownerWithLocations.getString("token"));
        userLocationPage.userToEnter.set(newUser);
        userLocationPage.openPage();
        userLocationPage.linkUnlinkLocationByIndex(0);
        userLocationPage.lastLocationUnlinkFromStaffOrLocationAdminErrorMessage();
    }

}
