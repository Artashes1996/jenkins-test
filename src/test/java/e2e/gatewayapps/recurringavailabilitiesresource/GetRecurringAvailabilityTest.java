package e2e.gatewayapps.recurringavailabilitiesresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.recurringavailabilitiesresource.RecurringAvailabilitiesHelper;
import helpers.appsapi.recurringavailabilitiesresource.payloads.Days;
import helpers.flows.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.UNLINK;
import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static helpers.appsapi.recurringavailabilitiesresource.payloads.Days.*;
import static utils.TestUtils.*;

public class GetRecurringAvailabilityTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private ResourceFlows resourceFlows;
    private RecurringAvailabilitiesFlows recurringAvailabilitiesFlows;

    private JSONObject organizationWithUsers;
    private String organizationId;
    private String locationId;


    @BeforeClass
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        resourceFlows = new ResourceFlows();
        recurringAvailabilitiesFlows = new RecurringAvailabilitiesFlows();
        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationWithUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5361")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void removeLocationFromUserAndCheckAvailability(Role role) {
        final String locationIdToUnlinkUserFrom = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Arrays.asList(locationId, locationIdToUnlinkUserFrom));

        final String resourceId = resourceFlows.getResourceIdFromUserId(organizationId, user.getString("id"));
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationIdToUnlinkUserFrom, resourceId);
        userFlows.linkUnlinkLocationsToUser(organizationId, user.getString("id"), Collections.singletonList(locationIdToUnlinkUserFrom), UNLINK);

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationIdToUnlinkUserFrom, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(is("[]"));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5362")
    @Test
    public void removeLocationFromResourceAndCheckAvailability() {
        final String locationIdToUnlinkUserFrom = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(locationId, locationIdToUnlinkUserFrom))
                .getString("id");

        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationIdToUnlinkUserFrom, resourceId);
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationIdToUnlinkUserFrom, resourceId, UNLINK);

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationIdToUnlinkUserFrom, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(is("[]"));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5363")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteUserAndCheckAvailability(Role role) {
        final String locationIdToSetAvailability = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Arrays.asList(locationId, locationIdToSetAvailability));

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(user.getString("id"));
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationIdToSetAvailability, resourceId);
        userFlows.deleteUser(organizationId, user.getString("id"));

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationIdToSetAvailability, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(is("[]"));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5364")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void changeOnlyRoleAndCheckAvailability(Role role) {
        final String locationIdToSetAvailability = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Arrays.asList(locationId, locationIdToSetAvailability));

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(user.getString("id"));
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "19:00");
        timeSlots.put(timeSlot);

        final Role roleToChange = role.equals(Role.STAFF) ? Role.ADMIN : Role.STAFF;
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationIdToSetAvailability, resourceId, DayOfWeek.MONDAY, timeSlots);
        userFlows.changeRoleOfUser(organizationId, user.getString("id"), roleToChange);

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationIdToSetAvailability, resourceId)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("dayOfWeek.get(0)", is(DayOfWeek.MONDAY.name()))
                .body("resourceId.get(0)", is(resourceId))
                .body("fromTime.get(0)", is(timeSlot.get(FROM)))
                .body("toTime.get(0)", is(timeSlot.get(TO)));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5365")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void changeLocationAndCheckAvailability(Role role) {
        final String locationIdToSetAvailability = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Arrays.asList(locationId, locationIdToSetAvailability));

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(user.getString("id"));
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "19:00");
        timeSlots.put(timeSlot);

        final Role roleToChange = role.equals(Role.STAFF) ? Role.ADMIN : Role.STAFF;
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, resourceId, DayOfWeek.THURSDAY, timeSlots);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationIdToSetAvailability, resourceId, DayOfWeek.MONDAY, timeSlots);
        userFlows.changeRoleAndLocationOfUser(organizationId, user.getString("id"), roleToChange, Collections.singletonList(locationId));

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("dayOfWeek", hasItem(DayOfWeek.THURSDAY.name()))
                .body("resourceId", hasItem(resourceId))
                .body("fromTime", hasItem(timeSlot.get(FROM)))
                .body("toTime", hasItem(timeSlot.get(TO)));

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationIdToSetAvailability, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(is("[]"));
    }

    @Xray(test = "PEG-5540", requirement = "PEG-4832")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void getListOfRecurringAvailabilitiesBySupportedRoles(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONArray recurringAvailabilityForWeek = recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("toTime", everyItem(is(recurringAvailabilityForWeek.getJSONObject(getRandomInt(recurringAvailabilityForWeek.length())).getString("toTime"))))
                .body("fromTime", everyItem(is(recurringAvailabilityForWeek.getJSONObject(getRandomInt(recurringAvailabilityForWeek.length())).getString("fromTime"))));
    }

    @Xray(test = "PEG-5565", requirement = "PEG-4832")
    @Test
    public void getListOfRecurringAvailabilitiesByUnsupportedRole() {
        final String staffToken = organizationWithUsers.getJSONObject(STAFF.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);

        RecurringAvailabilitiesHelper.getRecurringAvailability(staffToken, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-5566", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesManySlotsInOneDay() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONArray timeSlots = recurringAvailabilitiesFlows.createHourlyTimeSlots(5);
        final List<String> toTimes = new ArrayList<>();
        final List<String> fromTimes = new ArrayList<>();
        timeSlots.forEach(timeSlot -> {
            toTimes.add(((JSONObject) timeSlot).getString("to"));
            fromTimes.add(((JSONObject) timeSlot).getString("from"));
        });

        final DayOfWeek day = getRandomDay();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, resourceId, day, timeSlots);

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("fromTime.sort()", is(fromTimes))
                .body("toTime.sort()", is(toTimes));
    }

    @Xray(test = "PEG-5570", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesSameResourceDifferentTimeZoneLocation() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String locationId_1 = locationFlows.createLocationByUtcOffset(organizationId, -12).getString("id");
        final String locationId_2 = locationFlows.createLocationByUtcOffset(organizationId, 12).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(locationId_1, locationId_2))
                .getString("id");

        final DayOfWeek day = getRandomDay();
        final int timeSlotsCount = getRandomInt(1, 5);
        final JSONArray timeSlots = recurringAvailabilitiesFlows.createHourlyTimeSlots(timeSlotsCount);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId_1, resourceId, day, timeSlots);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId_2, resourceId, day, timeSlots);

        final List<String> toTimes = new ArrayList<>();
        final List<String> fromTimes = new ArrayList<>();
        timeSlots.forEach(timeSlot -> {
            toTimes.add(((JSONObject) timeSlot).getString("to"));
            fromTimes.add(((JSONObject) timeSlot).getString("from"));
        });

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId_1, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("size()", is(timeSlotsCount))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId_1)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("fromTime.sort()", is(fromTimes))
                .body("toTime.sort()", is(toTimes));

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId_2, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("size()", is(timeSlotsCount))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId_2)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())));
    }

    @Xray(test = "PEG-5576", requirement = "PEG-4832")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void getEmployeeTypeResourceRecurringAvailabilities(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final JSONObject invitedUser = userFlows.inviteUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final String invitedUserId = userFlows.getUserId(invitedUser.getString("email"), organizationId);
        final String employeeResourceId = DBHelper.getEmployeeResourceIdByUserId(invitedUserId);

        final DayOfWeek day = getRandomDay();
        final JSONArray timeslots = recurringAvailabilitiesFlows.createHourlyTimeSlots(1);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, employeeResourceId, day, timeslots);

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, employeeResourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(employeeResourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("toTime[0]", is(timeslots.getJSONObject(0).getString("to")))
                .body("fromTime[0]", is(timeslots.getJSONObject(0).getString("from")));
    }

    @Xray(test = "PEG-5572", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilityOfBlockedOrganization() {
        final JSONObject blockedOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String blockedOrganizationId = blockedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String blockedOrganizationLocationId = locationFlows.createLocation(blockedOrganizationId).getString("id");
        final Role randomRole = getRandomHigherAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : blockedOrganization.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(blockedOrganizationId, Collections.singletonList(blockedOrganizationLocationId))
                .getString("id");

        final DayOfWeek day = getRandomDay();
        final JSONArray timeslots = recurringAvailabilitiesFlows.createHourlyTimeSlots(1);
        recurringAvailabilitiesFlows.createRecurringAvailability(blockedOrganizationId, blockedOrganizationLocationId, resourceId, day, timeslots);
        organizationFlows.blockOrganization(blockedOrganizationId);

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, blockedOrganizationId, blockedOrganizationLocationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(blockedOrganizationId)))
                .body("locationId", everyItem(is(blockedOrganizationLocationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("toTime[0]", is(timeslots.getJSONObject(0).getString("to")))
                .body("fromTime[0]", is(timeslots.getJSONObject(0).getString("from")));
    }

    @Xray(test = "PEG-5574", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilityOfPausedOrganization() {
        final JSONObject pausedOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String pausedOrganizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String pausedOrganizationLocationId = locationFlows.createLocation(pausedOrganizationId).getString("id");
        final Role randomRole = getRandomHigherAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : pausedOrganization.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(pausedOrganizationId, Collections.singletonList(pausedOrganizationLocationId))
                .getString("id");

        final DayOfWeek day = getRandomDay();
        final JSONArray timeslots = recurringAvailabilitiesFlows.createHourlyTimeSlots(1);
        recurringAvailabilitiesFlows.createRecurringAvailability(pausedOrganizationId, pausedOrganizationLocationId, resourceId, day, timeslots);
        organizationFlows.pauseOrganization(pausedOrganizationId);

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, pausedOrganizationId, pausedOrganizationLocationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(pausedOrganizationId)))
                .body("locationId", everyItem(is(pausedOrganizationLocationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("toTime[0]", is(timeslots.getJSONObject(0).getString("to")))
                .body("fromTime[0]", is(timeslots.getJSONObject(0).getString("from")));
    }

    @Xray(test = "PEG-5573", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilityOfDeletedOrganization() {
        final JSONObject deletedOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String deletedOrganizationId = deletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");

        final String resourceId = resourceFlows.createActiveResource(deletedOrganizationId, Collections.singletonList(deletedOrganizationLocationId))
                .getString("id");

        final DayOfWeek day = getRandomDay();
        final JSONArray timeslots = recurringAvailabilitiesFlows.createHourlyTimeSlots(1);
        recurringAvailabilitiesFlows.createRecurringAvailability(deletedOrganizationId, deletedOrganizationLocationId, resourceId, day, timeslots);
        organizationFlows.deleteOrganization(deletedOrganizationId);

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationLocationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(deletedOrganizationId)))
                .body("locationId", everyItem(is(deletedOrganizationLocationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("toTime[0]", is(timeslots.getJSONObject(0).getString("to")))
                .body("fromTime[0]", is(timeslots.getJSONObject(0).getString("from")));
    }

    @Xray(test = "PEG-5569", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesOfWrongLocationResourcePair() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");

        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, newLocationId, resourceId, getRandomDay(),
                recurringAvailabilitiesFlows.createHourlyTimeSlots(1));

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(is("[]"));
    }

    @Xray(test = "PEG-5571", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesWhileExistingFixedAvailabilities() {
        final Role randomRole = getRandomOrganizationInviterRole();
        final String token = organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");

        final JSONArray recurringAvailabilityForWeek = recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);

        final FixedAvailabilitiesFlows fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();

        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusDays(1));

        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, date, "00:00", "23:59");

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(not(empty()))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek.size()", is(7))
                .body("toTime", everyItem(is(recurringAvailabilityForWeek.getJSONObject(getRandomInt(recurringAvailabilityForWeek.length())).getString("toTime"))))
                .body("fromTime", everyItem(is(recurringAvailabilityForWeek.getJSONObject(getRandomInt(recurringAvailabilityForWeek.length())).getString("fromTime"))));
    }

    @Xray(test = "PEG-5575", requirement = "PEG-4832")
    @Test
    public void getNonExistingRecurringAvailabilities() {
        final Role randomRole = getRandomInviterRole();
        final String token = organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/listOfRecurringAvailabilities.json"))
                .body(is("[]"));
    }

    @Xray(test = "PEG-5568", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesOfOtherLocationByLocationAdmin() {
        final String locationAdminToken = organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(otherLocationId)).getString("id");
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, otherLocationId, resourceId);

        RecurringAvailabilitiesHelper.getRecurringAvailability(locationAdminToken, organizationId, otherLocationId, resourceId)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-5567", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesOfNonExistingResource() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String nonExistingResourceId = UUID.randomUUID().toString();

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, nonExistingResourceId)
                .then()
                .statusCode(SC_OK)
                .body(is("[]"));
    }

    @Xray(test = "PEG-5819", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilityInactiveLocation() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String inactiveLocationId = locationFlows.createLocation(organizationId).getString("id");

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(userFlows.createUser(organizationId, STAFF,
                Collections.singletonList(inactiveLocationId)).getString("id"));
        final JSONArray timeSlots = recurringAvailabilitiesFlows.createHourlyTimeSlots(4);
        final DayOfWeek day = Days.getRandomDay();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, inactiveLocationId, resourceId, day, timeSlots);
        locationFlows.inactivateLocation(organizationId, inactiveLocationId);

        final List<String> toTimes = new ArrayList<>();
        final List<String> fromTimes = new ArrayList<>();

        timeSlots.forEach(timeSlot -> {
            toTimes.add(((JSONObject) timeSlot).getString("to"));
            fromTimes.add(((JSONObject) timeSlot).getString("from"));
        });

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, inactiveLocationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(not(empty()))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(inactiveLocationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("fromTime.sort()", is(fromTimes))
                .body("toTime.sort()", is(toTimes));
    }

    @Xray(test = "PEG-5820", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilityInactiveResource() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String inactiveResourceId = resourceFlows.createInactiveResource(organizationId,
                Collections.singletonList(locationId)).getString("id");

        final DayOfWeek day = getRandomDay();
        final JSONArray timeSlots = recurringAvailabilitiesFlows.createHourlyTimeSlots(1);

        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, inactiveResourceId, day, timeSlots);
        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, inactiveResourceId)
                .then()
                .statusCode(SC_OK)
                .body(not(empty()))
                .body("size()", is(1))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(inactiveResourceId)))
                .body("dayOfWeek", everyItem(is(day.name())))
                .body("fromTime[0]", is(timeSlots.getJSONObject(0).getString("from")))
                .body("toTime[0]", is(timeSlots.getJSONObject(0).getString("to")));
    }

    @Xray(test = "PEG-5821", requirement = "PEG-4832")
    @Test
    public void getRecurringAvailabilitiesWithUnavailableDay() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");

        final FixedAvailabilitiesFlows fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusDays(1));
        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, resourceId, date);
        final JSONArray recurringAvailabilities = recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);

        final List<String> toTimes = new ArrayList<>();
        final List<String> fromTimes = new ArrayList<>();

        recurringAvailabilities.forEach(timeSlot -> {
            toTimes.add(((JSONObject) timeSlot).getString("toTime"));
            fromTimes.add(((JSONObject) timeSlot).getString("fromTime"));
        });

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, resourceId)
                .then()
                .statusCode(SC_OK)
                .body(not(empty()))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("fromTime.sort()", is(fromTimes))
                .body("toTime.sort()", is(toTimes));
    }

    @Xray(test = "PEG-5822", requirement = "PEG-4832")
    @Test
    public void getDeletedRecurringAvailability() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String inactiveResourceId = resourceFlows.createInactiveResource(organizationId,
                Collections.singletonList(locationId)).getString("id");

        final DayOfWeek day = getRandomDay();
        final JSONArray timeSlots = recurringAvailabilitiesFlows.createHourlyTimeSlots(1);

        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, inactiveResourceId, day, timeSlots);

        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, inactiveResourceId, day, new JSONArray());

        RecurringAvailabilitiesHelper.getRecurringAvailability(token, organizationId, locationId, inactiveResourceId)
                .then()
                .statusCode(SC_OK)
                .body(not(empty()))
                .body(is("[]"));
    }

}
