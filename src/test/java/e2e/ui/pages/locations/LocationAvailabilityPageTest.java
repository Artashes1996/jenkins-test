package e2e.ui.pages.locations;

import configuration.Role;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.ui.pages.BasePageTest;
import helpers.flows.*;
import org.json.*;
import org.testng.annotations.*;
import pages.LocationsAvailabilityPage;
import pages.LocationsInformationPage;
import pages.SignInPage;
import utils.*;
import utils.commons.ToggleAction;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;

public class LocationAvailabilityPageTest extends BasePageTest {

    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private ServiceFlows serviceFlows;
    private RecurringAvailabilitiesFlows recurringAvailabilitiesFlows;

    private String organizationId;
    private String locationId;
    private String secondLocationId;
    private String userId;
    private JSONObject organizationWithUsers;
    private List<JSONObject> allEmployees;
    private List<JSONObject> employeesWithName;

    @BeforeClass
    public void setUp() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        serviceFlows = new ServiceFlows();
        recurringAvailabilitiesFlows = new RecurringAvailabilitiesFlows();
        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationWithUsers.getJSONObject("LOCATION").getString("id");
        userId = organizationWithUsers.getJSONObject(getRandomOrganizationRole().name()).getString("id");
        secondLocationId = locationFlows.createLocation(organizationId).getString("id");
        userFlows.createInactiveUsers(organizationId, getRandomOrganizationRole(), locationId, 2);
        userFlows.inviteUsers(organizationId, Collections.singletonList(locationId), getRandomOrganizationRole(), 2);
        final JSONArray users = userFlows.getListOfUsersByLinkedLocationsWithAscendingSorted(organizationId, Collections.singletonList(locationId));
        allEmployees = new ArrayList<>();
        for (int i = 0; i < users.length(); i++) {
            allEmployees.add(users.getJSONObject(i));
        }

        employeesWithName = allEmployees.stream().filter(employee ->
                !employee.isNull("firstName") && !employee.isNull("lastName")).collect(Collectors.toList());
    }

    @BeforeMethod
    final void init() {
        new SignInPage(browserToUse, versionToBe).openPage();
    }

    @Xray(requirement = "PEG-6635", test = "PEG-6965")
    @Test(dataProvider = "rolesWithLocation", dataProviderClass = RoleDataProvider.class)
    public void availabilityTabForUnsupportedRoles(Role role) {
        final LocationsInformationPage locationsInformationPage = role.equals(STAFF) ?
                new LocationsInformationPage(browserToUse, versionToBe, locationId, organizationWithUsers.getJSONObject(role.name()).getString("token"))
                : new LocationsInformationPage(browserToUse, versionToBe, secondLocationId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsInformationPage.openPage();
        locationsInformationPage.checkAvailabilityTabNotDisplayed();
    }

    @Xray(requirement = "PEG-6635", test = "PEG-6967")
    @Test
    public void emptyEmployeesList() {
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, secondLocationId, userId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, secondLocationId, userId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.checkEmptyEmployeeList();
    }

    @Xray(requirement = "PEG-6635", test = "PEG-6966")
    @Test
    public void seeListOfEmployees() {
        final Role role = getRandomInviterRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, locationId, userId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, locationId, userId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
//     TODO have a time when assertion failed should again review PEG-7234 after fixed
        locationsAvailabilityPage.checkEmployeesList(allEmployees);
    }

    @Xray(requirement = "PEG-6635", test = "PEG-6969")
    @Test
    public void searchEmployee() {
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, locationId, userId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, locationId, userId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        final JSONObject employee = employeesWithName.get(TestUtils.getRandomInt(employeesWithName.size()));
        locationsAvailabilityPage.searchEmployee(employee.getString("firstName"));
        locationsAvailabilityPage.checkEmployeeInfoByIndex(0, employee);
        locationsAvailabilityPage.searchEmployee(employee.getString("lastName"));
        locationsAvailabilityPage.checkEmployeeInfoByIndex(0, employee);
        locationsAvailabilityPage.searchEmployee(employee.getString("email"));
        locationsAvailabilityPage.checkEmployeeInfoByIndex(0, employee);
    }

    @Xray(requirement = "PEG-6705", test = "PEG-7104")
    @Test
    public void makeUnavailableDayAvailable() {
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, locationId, userId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, locationId, userId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        locationsAvailabilityPage.selectAvailabilityCheckboxByWeekday(randomDayOfWeek);
        locationsAvailabilityPage.checkIfNotAvailableIsNotDisplayed(randomDayOfWeek);
    }

    @Xray(requirement = "PEG-6705", test = "PEG-7106")
    @Test
    public void configureHours() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId)).getString("id");
        final Role role = getRandomAdminRole();
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, newLocationId, newUserId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.selectAvailabilityCheckboxByWeekday(randomDayOfWeek);
        locationsAvailabilityPage.selectHours("15:00", "16:00", randomDayOfWeek, 0);
        locationsAvailabilityPage.checkSuccessToast();
    }

    @Xray(requirement = "PEG-6705", test = "PEG-7147")
    @Test
    public void byDefaultState() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        userFlows.linkUnlinkLocationToUser(organizationId, userId, newLocationId, ToggleAction.LINK);
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, userId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, newLocationId, userId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.byDefaultCheckboxesIsNotSelected();
        locationsAvailabilityPage.selectAvailabilityCheckboxByWeekday(randomDayOfWeek);
        locationsAvailabilityPage.byDefaultFromAndToDateValues(randomDayOfWeek, 0);
    }

    @Xray(requirement = "PEG-6705", test = "PEG-7147")
    @Test
    public void checkOrderOfWeekDays() {
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, locationId, userId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, locationId, userId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.checkOrderOfWeekdays();
    }

    @Xray(requirement = "PEG-6716", test = "PEG-7230")
    @Test
    public void newIntervalButton() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId)).getString("id");
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, newLocationId, newUserId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.checkIfNotAvailableIsNotDisplayed(randomDayOfWeek);
        locationsAvailabilityPage.checkTimesByWeekdayAndRow(randomDayOfWeek, 0, "9:00 am", "5:00 pm");
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.checkTimesByWeekdayAndRow(randomDayOfWeek, 1, "6:00 pm", "7:00 pm");
    }

    @Xray(requirement = "PEG-6716", test = "PEG-7231")
    @Test
    public void removeIntervalButton() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId)).getString("id");
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, newLocationId, newUserId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.clickOnRemoveIntervalButtonByWeekDayAndRow(randomDayOfWeek, 1);
        locationsAvailabilityPage.checkTimesByWeekdayAndRow(randomDayOfWeek, 0, "9:00 am", "5:00 pm");
        locationsAvailabilityPage.clickOnRemoveIntervalButtonByWeekDayAndRow(randomDayOfWeek, 0);
        locationsAvailabilityPage.checkIfNotAvailableIsDisplayed(randomDayOfWeek);
    }

    @Xray(requirement = "PEG-6716", test = "PEG-7232")
    @Test
    public void startTimeLaterThanEndTime() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId)).getString("id");
        userFlows.linkUnlinkLocationToUser(organizationId, userId, newLocationId, ToggleAction.LINK);
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, newLocationId, newUserId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.selectHours("10:00", "09:30", randomDayOfWeek, 0);
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                0, "The end time must be later than the start time");
        locationsAvailabilityPage.selectHours("11:00", "11:00", randomDayOfWeek, 0);
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                0, "The end time must be later than the start time");
    }

    @Xray(requirement = "PEG-6716", test = "PEG-7233")
    @Test
    public void timeSlotsOverlap() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId)).getString("id");
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.selectHours("10:30", "10:45", randomDayOfWeek, 1);
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                0, "The selected times overlap with another time interval");
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                1, "The selected times overlap with another time interval");
        locationsAvailabilityPage.selectHours("17:00", "18:00", randomDayOfWeek, 1);
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                0, "The selected times overlap with another time interval");
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                1, "The selected times overlap with another time interval");
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.selectHours("10:30", "11:00", randomDayOfWeek, 2);
        locationsAvailabilityPage.clickOnRemoveIntervalButtonByWeekDayAndRow(randomDayOfWeek,1);
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                0, "The selected times overlap with another time interval");
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                1, "The selected times overlap with another time interval");
    }

    @Xray(requirement = "PEG-6716", test = "PEG-7235")
    @Test
    public void displayOldChanges() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(newLocationId)).getString("id");
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, newLocationId, newUserId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, newLocationId, newUserId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.selectHours("08:00", "05:45", randomDayOfWeek, 1);
        locationsAvailabilityPage.checkSuccessToast();
        locationsAvailabilityPage.refreshPage();
        locationsAvailabilityPage.checkTimesByWeekdayAndRow(randomDayOfWeek, 0, "9:00 am", "5:00 pm");
        locationsAvailabilityPage.checkTimesByWeekdayAndRow(randomDayOfWeek, 1, "6:00 pm", "7:00 pm");
    }

    @Xray(requirement = "PEG-6716", test = "PEG-7236")
    @Test
    public void timesOverlapWithAnotherLocation() {
        final List<String> locations = Arrays.asList(locationFlows.createLocation(organizationId).getString("id"),
                locationFlows.createLocation(organizationId).getString("id"));
        final String firstLocationId = locations.get(0);
        final String secondLocationId = locations.get(1);
        final JSONObject newUser = userFlows.createUser(organizationId, getRandomOrganizationRole(), locations);
        final String newResourceId = newUser.getString("resourceId");
        final String newUserId = newUser.getString("id");
        final String serviceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkServiceToLocationAndUser(organizationId, firstLocationId, serviceId, newUserId);
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final Role role = getRandomAdminRole();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, firstLocationId, newResourceId, randomDayOfWeek, "10:00", "11:00");
        final LocationsAvailabilityPage locationsAvailabilityPage = role.equals(SUPPORT) ?
                new LocationsAvailabilityPage(browserToUse, versionToBe, organizationId, secondLocationId, newResourceId, supportToken)
                : new LocationsAvailabilityPage(browserToUse, versionToBe, secondLocationId, newResourceId, organizationWithUsers.getJSONObject(role.name()).getString("token"));
        locationsAvailabilityPage.openPage();
        locationsAvailabilityPage.clickOnNewIntervalButtonByWeekDay(randomDayOfWeek);
        locationsAvailabilityPage.selectHours("10:30", "11:30", randomDayOfWeek, 0);
        locationsAvailabilityPage.checkErrorMessageByWeekdayAndRow(randomDayOfWeek,
                0, "This time conflicts with the availability in another location");
    }

}
