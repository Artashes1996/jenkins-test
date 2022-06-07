package e2e.gatewayapps.availabletimeslots;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.availabletimeslots.AvailableTimeSlotsHelper;

import static helpers.appsapi.availabletimeslots.payloads.GetAvailableTimeSlotsListBody.*;

import helpers.appsapi.availabletimeslots.payloads.GetAvailableTimeSlotsListBody;
import helpers.appsapi.fixedavailabilitiesresource.FixedAvailabilitiesHelper;
import helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody;
import helpers.appsapi.recurringavailabilitiesresource.payloads.Days;
import helpers.appsapi.support.locationresource.payloads.TimeZones;
import helpers.flows.*;

import io.restassured.response.Response;
import org.json.*;
import org.testng.annotations.*;
import utils.TestUtils;
import utils.Xray;
import utils.commons.ToggleAction;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

import static configuration.Role.*;
import static helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody.*;
import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.TO;
import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.FROM;
import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.TIME_SLOTS;

import static helpers.appsapi.recurringavailabilitiesresource.payloads.Days.getRandomDay;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.apache.hc.core5.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.LINK;

public class GetListOfAvailableTimeSlotsTest extends BaseTest {

    private String organizationId;
    private String locationId;
    private String resourceId;
    private String serviceId;
    private int serviceDuration;

    private JSONObject organizationAndUsers;

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private ResourceFlows resourceFlows;
    private ServiceFlows serviceFlows;
    private UserFlows userFlows;
    private FixedAvailabilitiesFlows fixedAvailabilitiesFlows;
    private RecurringAvailabilitiesFlows recurringAvailabilitiesFlows;
    private AvailabilityTimeSlotsFlows availabilityTimeSlotsFlows;
    private AppointmentsFlow appointmentsFlow;

    private FixedAvailabilityUpsertBody fixedAvailabilityUpsertBody;
    private GetAvailableTimeSlotsListBody getAvailableTimeSlotsListBody;
    private int oneHourInSeconds;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        resourceFlows = new ResourceFlows();
        serviceFlows = new ServiceFlows();
        userFlows = new UserFlows();
        fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();
        recurringAvailabilitiesFlows = new RecurringAvailabilitiesFlows();
        availabilityTimeSlotsFlows = new AvailabilityTimeSlotsFlows();
        appointmentsFlow = new AppointmentsFlow();

        fixedAvailabilityUpsertBody = new FixedAvailabilityUpsertBody();
        getAvailableTimeSlotsListBody = new GetAvailableTimeSlotsListBody();
        oneHourInSeconds = 3600;

        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, oneHourInSeconds);
        serviceId = service.getString("id");
        serviceDuration = service.getInt("duration");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, resourceId, Collections.singletonList(serviceId), LINK);
    }

    @Xray(test = "PEG-5524", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsBlockedOrganization() {
        final JSONObject blockedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : blockedOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String blockedOrganizationId = blockedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String blockedOrganizationLocationId = blockedOrganizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        final String blockedOrganizationServiceId = serviceFlows.createService(blockedOrganizationId).getString("id");
        final String blockedOrganizationResourceId = resourceFlows.createActiveResource(blockedOrganizationId, Collections.singletonList(blockedOrganizationLocationId)).getString("id");
        serviceFlows.linkLocationsToService(blockedOrganizationId, blockedOrganizationServiceId, Collections.singletonList(blockedOrganizationLocationId));
        resourceFlows.linkUnlinkServicesToResource(blockedOrganizationId, blockedOrganizationLocationId, blockedOrganizationResourceId, Collections.singletonList(blockedOrganizationServiceId), LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(blockedOrganizationResourceId);
        recurringAvailabilitiesFlows.createRecurringAvailability(blockedOrganizationId, blockedOrganizationLocationId, blockedOrganizationResourceId);
        organizationFlows.blockOrganization(blockedOrganizationId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, blockedOrganizationId, blockedOrganizationLocationId, blockedOrganizationServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5525", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsPausedOrganization() {
        final JSONObject pauseOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : pauseOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String pausedOrganizationId = pauseOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String pausedOrganizationLocationId = pauseOrganizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        final String pausedOrganizationServiceId = serviceFlows.createService(pausedOrganizationId).getString("id");
        final String pausedOrganizationResourceId = resourceFlows.createActiveResource(pausedOrganizationId, Collections.singletonList(pausedOrganizationLocationId)).getString("id");
        serviceFlows.linkLocationsToService(pausedOrganizationId, pausedOrganizationServiceId, Collections.singletonList(pausedOrganizationLocationId));
        resourceFlows.linkUnlinkServicesToResource(pausedOrganizationId, pausedOrganizationLocationId, pausedOrganizationResourceId, Collections.singletonList(pausedOrganizationServiceId), LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(pausedOrganizationResourceId);
        recurringAvailabilitiesFlows.createRecurringAvailability(pausedOrganizationId, pausedOrganizationLocationId, pausedOrganizationResourceId);
        organizationFlows.pauseOrganization(pausedOrganizationId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, pausedOrganizationId, pausedOrganizationLocationId, pausedOrganizationServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5528", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsDeletedOrganization() {
        final JSONObject deletedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String deletedOrganizationId = deletedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = deletedOrganizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        final String deletedOrganizationServiceId = serviceFlows.createService(deletedOrganizationId).getString("id");
        final String deletedOrganizationResourceId = resourceFlows.createActiveResource(deletedOrganizationId, Collections.singletonList(deletedOrganizationLocationId)).getString("id");
        serviceFlows.linkLocationsToService(deletedOrganizationId, deletedOrganizationServiceId, Collections.singletonList(deletedOrganizationLocationId));
        resourceFlows.linkUnlinkServicesToResource(deletedOrganizationId, deletedOrganizationLocationId, deletedOrganizationResourceId, Collections.singletonList(deletedOrganizationServiceId), LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(deletedOrganizationResourceId);
        recurringAvailabilitiesFlows.createRecurringAvailability(deletedOrganizationId, deletedOrganizationLocationId, deletedOrganizationResourceId);
        organizationFlows.deleteOrganization(deletedOrganizationId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationLocationId, deletedOrganizationServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5530", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsInvalidMonth() {
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(resourceId);
        body.put(MONTH, "invalid_month");
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(SUPPORT_TOKEN, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(test = "PEG-5531", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsInvalidTimezone() {
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(resourceId);
        body.put(REPRESENTATION_TIME_ZONE, "invalid_representation_timezone");
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(SUPPORT_TOKEN, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5532", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsWithEmptyBody() {
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(SUPPORT_TOKEN, organizationId, locationId, serviceId, new JSONObject())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5534", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsInactiveLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String inactiveLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(inactiveLocationId)).getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(inactiveLocationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, inactiveLocationId, newResourceId, Collections.singletonList(serviceId), LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, inactiveLocationId, newResourceId);
        locationFlows.inactivateLocation(organizationId, inactiveLocationId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, inactiveLocationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5535", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsInactiveService() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String inactiveServiceId = serviceFlows.createInactiveService(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkLocationsToService(organizationId, inactiveServiceId, Collections.singletonList(locationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId, Collections.singletonList(inactiveServiceId), LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, newResourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, inactiveServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
        ;
    }

    @Xray(test = "PEG-5537", requirement = "PEG-4849")
    @Test
    public void serviceDurationIsMoreThanResourceAvailability() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final DayOfWeek day = getRandomDay();
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "09:04");
        timeSlots.put(timeSlot);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, newResourceId, day, timeSlots);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5538", requirement = "PEG-4849")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getListOfAvailableTimeSlotsFixedAvailabilities(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final String currentDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId, currentDate, "09:00", "10:00");
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        final LocalDateTime formatTime = LocalDateTime.from(dateTimeFormatter.parse(currentDate + "T" + "09:00:00"));
        final String toTime = dateTimeFormatter.format(formatTime);
        final String toDate = dateTimeFormatter.format(LocalDateTime.parse(toTime).plusSeconds(serviceDuration));
        final String fromDate = currentDate + "T" + "09:00:00";
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from[0]", is(fromDate))
                .body("to[0]", is(toDate));
    }

    @Xray(test = "PEG-5539", requirement = "PEG-4849")
    @Test(dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void getListOfAvailableTimeSlotsRecurringAvailabilities(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "08:00");
        timeSlot.put(TO, "09:00");
        timeSlots.put(timeSlot);
        final String randomDay = String.valueOf(Days.getRandomDay());
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, newResourceId, DayOfWeek.valueOf(randomDay), timeSlots);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        final List<String> dates = TestUtils.getDaysByYearMonthAndWeekDay(Year.now().plusYears(1), LocalDateTime.now().getMonth(), randomDay);
        final ArrayList<String> fromFormattedDates = new ArrayList<>();
        final ArrayList<String> toFormattedDates = new ArrayList<>();
        for (String date : dates) {
            fromFormattedDates.add(LocalDateTime.parse(date + "T08:00") + ":00");
            toFormattedDates.add(LocalDateTime.parse(date + "T09:00") + ":00");
        }
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from[0]", is(fromFormattedDates.get(0)))
                .body("to[0]", is(toFormattedDates.get(0)));
    }

    @Xray(test = "PEG-5550", requirement = "PEG-4849")
    @Test
    public void getListInCaseOfNoticeTimeBeforeTheFirstAvailableTimeSlot() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String newServiceId = serviceFlows.createServiceByDuration(organizationId, 600).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(locationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(newServiceId), LINK);
        final JSONObject fixedAvailabilityBody = fixedAvailabilityUpsertBody.bodyBuilder(newResourceId);
        final LocalDateTime timeNow = LocalDateTime.now(ZoneId.of(TimeZones.getTimeZoneByUtcOffset(0)));
        final String firstTimeSlotFromDateWithSeconds = timeNow.plusMinutes(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        final String firstTimeSlotToDateWithSeconds = timeNow.plusMinutes(50).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        final String secondTimeSlotFromDateWithSeconds = timeNow.plusMinutes(90).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) + ":00";
        final String secondTimeSlotToDateWithSeconds = timeNow.plusMinutes(100).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) + ":00";

        final String firstTimeSlotFromDate = LocalDateTime.parse(firstTimeSlotFromDateWithSeconds).format((DateTimeFormatter.ofPattern("HH:mm")));
        final String firstTimeSlotToDate = LocalDateTime.parse(firstTimeSlotToDateWithSeconds).format((DateTimeFormatter.ofPattern("HH:mm")));
        final String secondTimeSlotFromDate = LocalDateTime.parse(secondTimeSlotFromDateWithSeconds).format((DateTimeFormatter.ofPattern("HH:mm")));
        final String secondTimeSlotToDate = LocalDateTime.parse(secondTimeSlotToDateWithSeconds).format((DateTimeFormatter.ofPattern("HH:mm")));

        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot_1 = new JSONObject();
        timeSlot_1.put(FROM, firstTimeSlotFromDate);
        timeSlot_1.put(TO, firstTimeSlotToDate);
        final JSONObject timeSlot_2 = new JSONObject();
        timeSlot_2.put(FROM, secondTimeSlotFromDate);
        timeSlot_2.put(TO, secondTimeSlotToDate);
        timeSlots.put(timeSlot_1);
        timeSlots.put(timeSlot_2);

        fixedAvailabilityBody.put(TIME_SLOTS, timeSlots);
        fixedAvailabilityBody.put(DATE, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now()));
        fixedAvailabilityBody.put(FixedAvailabilityUpsertBody.RESOURCE_ID, newResourceId);
        FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, fixedAvailabilityBody);

        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        body.put(YEAR, LocalDateTime.now().getYear());
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("from[0]", is(secondTimeSlotFromDateWithSeconds))
                .body("to[0]", is(secondTimeSlotToDateWithSeconds));
    }

    @Xray(test = "PEG-5553", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsInactiveResource() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String inactiveResourceId = resourceFlows.createInactiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, inactiveResourceId, Collections.singletonList(serviceId), LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(inactiveResourceId);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, inactiveResourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5554", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsInvitedUserResource() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String email = userFlows.inviteUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("email");
        final String userId = DBHelper.getUserIdByEmail(email);
        final String employeeResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(locationId));
        userFlows.linkUnlinkUserToLocationService(organizationId, userId, locationId, newServiceId, LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(employeeResourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5555", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsFixedAvailabilitiesEmployeeTypeResource() {
        final Role role = getRandomOrganizationRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final String employeeResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        final JSONObject serviceObject = serviceFlows.createService(organizationId);
        final String newServiceId = serviceObject.getString("id");
        final int newServiceDuration = serviceObject.getInt("duration");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(locationId));
        userFlows.linkUnlinkUserToLocationService(organizationId, userId,
                locationId, newServiceId, LINK);
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        final String from = "09:00";
        final String to = "09:35";
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, employeeResourceId, date, from, to);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(employeeResourceId);
        final String fromDate = date + "T" + "09:00:00";
        final String toDate = LocalDateTime.parse(date + "T" + "09:00:00").plusSeconds(newServiceDuration).toString();
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId,
                        newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from[0]", is(fromDate))
                .body("to[0]", is(toDate));
    }

    @Xray(test = "PEG-5556", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsWithOtherRepresentationTimeZone() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final String currentDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId, currentDate, "09:00", "10:00");
        final JSONObject searchBody = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        searchBody.put(REPRESENTATION_TIME_ZONE, TimeZones.getTimeZoneByUtcOffset(1));
        final String fromDate = currentDate + "T" + "10:00:00";
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final String toDate = LocalDateTime.parse(currentDate + "T10:00", formatter).plusSeconds(serviceDuration) + ":00";
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from[0]", is(fromDate))
                .body("to[0]", is(toDate));
    }

    @Xray(test = "PEG-5579", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsNonExistingOrganizationId() {
        final String fakeOrganizationId = UUID.randomUUID().toString();
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(resourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(SUPPORT_TOKEN, fakeOrganizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5580", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsNonExistingLocationId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String fakeLocationId = UUID.randomUUID().toString();
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(resourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, fakeLocationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5581", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsNonExistingServiceId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String fakeServiceId = UUID.randomUUID().toString();
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(resourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, fakeServiceId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5582", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsNonExistingResourceId() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String fakeResourceId = UUID.randomUUID().toString();
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(fakeResourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5536", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsNoRecurringAndFixedAvailabilities() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, newResourceId, serviceId, LINK);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(resourceId);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5801", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsMakeDayUnavailable() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final LocalDateTime localDateTime = LocalDateTime.now();
        final String day = localDateTime.getDayOfWeek().toString();
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "11:00");
        timeSlots.put(timeSlot);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId,
                newResourceId, DayOfWeek.valueOf(day), timeSlots);
        final List<String> days = TestUtils.getDaysByYearMonthAndWeekDay(Year.of(localDateTime.getYear() + 1), localDateTime.getMonth(), day);
        final String fixedAvailabilityDayDate = days.get(0);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId, fixedAvailabilityDayDate, "09:00", "10:00");
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        assertNotEquals(availabilityTimeSlotsFlows.getListOfAvailableTimeSlots(organizationId, locationId,
                serviceId, body).length(), 0);
        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, newResourceId, fixedAvailabilityDayDate);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(test = "PEG-5802", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsRemovingFixedAvailabilities() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newServiceId = serviceFlows.createServiceByDuration(organizationId, 600).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(locationId));
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(newServiceId), LINK);
        final LocalDateTime localDateTime = LocalDateTime.now();
        final String day = localDateTime.getDayOfWeek().toString();
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "09:10");
        timeSlots.put(timeSlot);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId,
                newResourceId, DayOfWeek.valueOf(day), timeSlots);
        final List<String> days = TestUtils.getDaysByYearMonthAndWeekDay(Year.of(localDateTime.getYear() + 1), localDateTime.getMonth(), day);
        final String fixedAvailabilityDayDate = days.get(0);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId, fixedAvailabilityDayDate, "09:00", "10:00");
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        fixedAvailabilitiesFlows.eraseFixedAvailability(organizationId, locationId, newResourceId, fixedAvailabilityDayDate);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, newServiceId, body)
                .then()
                .statusCode(SC_OK)
//               TODO time validation part
                .body("size()", greaterThanOrEqualTo(4));
    }

    @Xray(test = "PEG-5804", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsRemovingRecurringAvailability() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "19:00");
        timeSlots.put(timeSlot);
        final DayOfWeek randomDay = Days.getRandomDay();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, newResourceId, randomDay, timeSlots);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        final JSONArray emptyTimeSlots = new JSONArray();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, newResourceId,
                randomDay, emptyTimeSlots);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5834", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsResourceUnlinkedToLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId, date, "09:00", "10:00");
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationId, newResourceId, ToggleAction.UNLINK);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5835", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsServiceUnlinkedToLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(locationId));
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(newServiceId), LINK);
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId, date, "09:00", "10:00");
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        serviceFlows.unlinkLocationsFromService(organizationId, newServiceId, Collections.singletonList(locationId));
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5944", requirement = "4849")
    @Test(dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void getListOfAvailableTimeSlotsRecurringAvailabilitiesEmployeeTypeResource(Role role) {
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final String employeeResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        userFlows.linkUnlinkUserToLocationService(organizationId, userId, locationId, serviceId, LINK);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        final LocalDateTime timeNow = LocalDateTime.now();
        final String fromDate = timeNow.format(DateTimeFormatter.ofPattern("HH:mm"));
        final int timeSlotDuration = 90;
        final String toDate = timeNow.plusMinutes(timeSlotDuration).format(DateTimeFormatter.ofPattern("HH:mm"));
        timeSlot.put(FROM, fromDate);
        timeSlot.put(TO, toDate);
        timeSlots.put(timeSlot);
        final LocalDateTime currentDate = timeNow.plusYears(1);
        final DayOfWeek day = currentDate.getDayOfWeek();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, employeeResourceId, DayOfWeek.valueOf(day.name()), timeSlots);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(employeeResourceId);
        final List<String> days = TestUtils.getDaysByYearMonthAndWeekDay(Year.of(currentDate.getYear()), currentDate.getMonth(), String.valueOf(day));
        final String formattedFromDate = days.get(0) + "T" + fromDate + ":00";
        final String formattedToDate = timeNow.plusSeconds(serviceDuration).format(DateTimeFormatter.ofPattern("HH:mm"));
        final String formattedToDayWithTime = days.get(0) + "T" + formattedToDate + ":00";
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId,
                        serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from[0]", is(formattedFromDate))
                .body("to[0]", is(formattedToDayWithTime));
    }

    @Xray(test = "PEG-5865", requirement = "PEG-4849")
    @Test
    public void getListOfAvailableTimeSlotsPastYearAndPastMonth() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServicesToResource(organizationId, locationId, newResourceId,
                Collections.singletonList(serviceId), LINK);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        final String fromDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        final String toDate = LocalDateTime.now().plusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm"));
        timeSlot.put(FROM, fromDate);
        timeSlot.put(TO, toDate);
        timeSlots.put(timeSlot);
        final DayOfWeek randomDay = Days.getRandomDay();
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId,
                newResourceId, randomDay, timeSlots);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        body.put(MONTH, LocalDateTime.now().getMonth().minus(2));
        body.put(YEAR, Year.now().minusYears(1));
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(0));
    }

    @Xray(test = "PEG-5866", requirement = "PEG-4849")
    @Test
    public void seeOnlyTimeslotsThatAreGreaterThanServiceDuration() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newServiceId = serviceFlows.createServiceByDuration(organizationId, 840).getString("id");
        serviceFlows.linkLocationsToService(organizationId, newServiceId, Collections.singletonList(locationId));
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        resourceFlows.linkUnlinkServiceToResource(organizationId, locationId, newResourceId, newServiceId, LINK);
        final LocalDateTime randomDay = LocalDateTime.now().plusYears(1).plusMonths(1);
        final String month = String.valueOf(randomDay.getMonth());
        final String year = String.valueOf(randomDay.getYear());
        final String date = randomDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        final JSONObject fixedAvailabilityBody = new JSONObject();
        fixedAvailabilityBody.put(DATE, date);
        fixedAvailabilityBody.put(GetAvailableTimeSlotsListBody.RESOURCE_ID, newResourceId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot1 = new JSONObject();
        timeSlot1.put(FROM, "09:00");
        timeSlot1.put(TO, "09:15");
        timeSlots.put(timeSlot1);
        final JSONObject timeSlot2 = new JSONObject();
        timeSlot2.put(FROM, "09:20");
        timeSlot2.put(TO, "09:25");
        timeSlots.put(timeSlot2);
        final JSONObject timeSlot3 = new JSONObject();
        timeSlot3.put(FROM, "23:00");
        timeSlot3.put(TO, "23:30");
        timeSlots.put(timeSlot3);
        fixedAvailabilityBody.put(TIME_SLOTS, timeSlots);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, fixedAvailabilityBody);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        body.put(MONTH, month);
        body.put(YEAR, year);
        final List<String> formattedFromDates = new ArrayList<>(Arrays.asList(date + "T09:00:00", date + "T23:00:00", date + "T23:14:00"));
        final List<String> formattedToDates = new ArrayList<>(Arrays.asList(date + "T09:14:00", date + "T23:14:00", date + "T23:28:00"));
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(3))
                .body("from", is(formattedFromDates))
                .body("to", is(formattedToDates));
    }

    @Xray(test = "PEG-5878", requirement = "PEG-4849")
    @Test
    public void seeSingleDayAvailableTimeSlots() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String newServiceId = serviceFlows.createServiceByDuration(organizationId, 3600).getString("id");
        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, newServiceId, newResourceId);
        final LocalDateTime randomDay = LocalDateTime.now().plusYears(1).plusMonths(1);
        final String month = String.valueOf(randomDay.getMonth());
        final String year = String.valueOf(randomDay.getYear());
        final String date = randomDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        final JSONObject fixedAvailabilityBody = new JSONObject();
        fixedAvailabilityBody.put(DATE, date);
        fixedAvailabilityBody.put(GetAvailableTimeSlotsListBody.RESOURCE_ID, newResourceId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "00:00");
        timeSlot.put(TO, "23:59");
        timeSlots.put(timeSlot);
        fixedAvailabilityBody.put(TIME_SLOTS, timeSlots);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, fixedAvailabilityBody);
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        body.put(MONTH, month);
        body.put(YEAR, year);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(23));
    }

    @Xray(test = "PEG-5879", requirement = "PEG-4849")
    @Test
    public void fixedAvailabilitiesOverrideRecurringAvailabilities() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String newServiceId = serviceFlows.createServiceByDuration(organizationId, 1200).getString("id");
        serviceFlows.linkServiceToLocationAndResource(organizationId, locationId, newServiceId, newResourceId);
        final LocalDateTime randomDay = LocalDateTime.now().plusYears(1).plusMonths(1);
        final Month month = Month.valueOf(String.valueOf(randomDay.getMonth()));
        final Year year = Year.of(randomDay.getYear());
        final DayOfWeek weekDay = Days.getRandomDay();
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "09:30");
        timeSlots.put(timeSlot);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, newResourceId,
                weekDay, timeSlots);
        final List<String> days = TestUtils.getDaysByYearMonthAndWeekDay(year, month, String.valueOf(weekDay));
        final List<String> formattedFromDays = new ArrayList<>();
        final List<String> formattedToDays = new ArrayList<>();
        for (String day : days) {
            fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, newResourceId,
                    day, "10:00", "10:30");
            formattedFromDays.add(day + "T" + "10:00" + ":00");
            formattedToDays.add(day + "T" + "10:20" + ":00");
        }
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        body.put(MONTH, month);
        body.put(YEAR, year);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId,
                        newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from", is(formattedFromDays))
                .body("to", is(formattedToDays));
    }


    @Xray(test = "PEG-5898", requirement = "PEG-4849")
    @Test
    public void seeAvailableTimeSlotsForFirstLocation() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String firstLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String secondLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(firstLocationId, secondLocationId)).getString("id");
        final String newServiceId = serviceFlows.createServiceByDuration(organizationId, 1200).getString("id");
        serviceFlows.linkServiceToLocationAndResource(organizationId, firstLocationId, newServiceId, newResourceId);
        serviceFlows.linkServiceToLocationAndResource(organizationId, secondLocationId, newServiceId, newResourceId);
        final LocalDateTime randomDay = LocalDateTime.now().plusYears(1).plusMonths(1);
        final Month month = Month.valueOf(String.valueOf(randomDay.getMonth()));
        final Year year = Year.of(randomDay.getYear());
        final DayOfWeek weekDay = Days.getRandomDay();
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "09:30");
        timeSlots.put(timeSlot);
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, firstLocationId, newResourceId,
                weekDay, timeSlots);
        final List<String> days = TestUtils.getDaysByYearMonthAndWeekDay(year, month, String.valueOf(weekDay));
        final List<String> formattedFromDays = new ArrayList<>();
        final List<String> formattedToDays = new ArrayList<>();
        for (String day : days) {
            formattedFromDays.add(day + "T" + "09:00" + ":00");
            formattedToDays.add(day + "T" + "09:20" + ":00");
        }
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, secondLocationId, newResourceId,
                days.get(1), "10:00", "10:30");
        final JSONObject body = getAvailableTimeSlotsListBody.bodyBuilder(newResourceId);
        body.put(MONTH, month);
        body.put(YEAR, year);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, firstLocationId,
                        newServiceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("from", is(formattedFromDays))
                .body("to", is(formattedToDays));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6588")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingRecurringAndFixedAvailabilitiesForNextYear() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds * 4;
        final LocalDateTime requestDateTime = LocalDateTime.now().plusYears(1);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));

        final String userResourceId = resourceFlows.getResourceIdFromUserId(organizationId, user.getString("id"));
        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, serviceId, user.getString("id"));
        final String requestDateValue = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDateTime);

        final JSONArray recurringAvailabilityForWeek = recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, userResourceId);
        final JSONObject fixedAvailabilityUserResource = fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, userResourceId, requestDateValue, "12:00", "17:00");
        final List<Map<String, String>> userResourceTimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotForWeeklyRecurringAndFixedAvailability(fixedAvailabilityUserResource, recurringAvailabilityForWeek, serviceDuration, requestDateValue);

        final JSONObject resource1 = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resource1.getString("id"));
        final JSONObject fixedAvailabilityResource1 = fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resource1.getString("id"), requestDateValue, "08:00", "13:00");
        final List<Map<String, String>> resource1TimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotsForFixedAvailability(fixedAvailabilityResource1, serviceDuration);

        final JSONObject resource2 = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId));
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resource2.getString("id"));
        final JSONObject recurringAvailabilityResource2 = recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, resource2.getString("id"), DayOfWeek.MONDAY, "10:00", "15:00");
        final List<Map<String, String>> resource2TimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotsForSingleRecurringAvailability(recurringAvailabilityResource2, serviceDuration, requestDateValue);

        final List<Map<String, String>> allTimeSlots = Stream.of(userResourceTimeSlots, resource1TimeSlots, resource2TimeSlots)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        availabilityTimeSlotsFlows.sortTimeslots(allTimeSlots);

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(allTimeSlots.size()))
                .body("", is(allTimeSlots));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6589")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingRecurringAndFixedAvailabilitiesForCurrentYear() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds * 4;
        final LocalDateTime requestDateTime = LocalDateTime.now().plusDays(1);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));

        final String userResourceId = resourceFlows.getResourceIdFromUserId(organizationId, user.getString("id"));
        serviceFlows.linkServiceToLocationAndUser(organizationId, locationId, serviceId, user.getString("id"));
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDateTime);
        final JSONArray recurringAvailabilityForWeek = recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, userResourceId);
        final JSONObject fixedAvailabilityUserResource = fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, userResourceId, requestDateString, "12:00", "17:00");
        final List<Map<String, String>> userResourceTimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotForWeeklyRecurringAndFixedAvailability(fixedAvailabilityUserResource, recurringAvailabilityForWeek, serviceDuration, requestDateString);

        availabilityTimeSlotsFlows.sortTimeslots(userResourceTimeSlots);

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        body.put(YEAR, Year.now());
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(userResourceTimeSlots.size()))
                .body("", is(userResourceTimeSlots));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6590")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingRecurringAndFixedAvailabilitiesLessThanServiceDuration() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds * 3;
        final LocalDateTime requestDateTime = LocalDateTime.now().plusYears(1);
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDateTime);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));
        final JSONObject user1 = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));
        final JSONObject user2 = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));

        final String userResource1Id = resourceFlows.getResourceIdFromUserId(organizationId, user1.getString("id"));
        serviceFlows.linkServiceToUser(organizationId, locationId, serviceId, user1.getString("id"));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, userResource1Id, requestDateString, "12:00", "12:30");

        final String userResource2Id = resourceFlows.getResourceIdFromUserId(organizationId, user2.getString("id"));
        serviceFlows.linkServiceToUser(organizationId, locationId, serviceId, user2.getString("id"));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, userResource2Id, requestDateString, "13:00", "13:30");
        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, userResource2Id, DayOfWeek.MONDAY, "10:00", "11:00");

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(0));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6590")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingRecurringAndFixedAvailabilitiesLessThanService() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds;
        final LocalDateTime requestDateTime = LocalDateTime.now().plusYears(1);
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDateTime);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));
        final JSONObject user = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId));

        final String userResourceId = resourceFlows.getResourceIdFromUserId(organizationId, user.getString("id"));
        serviceFlows.linkServiceToUser(organizationId, locationId, serviceId, user.getString("id"));
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, userResourceId, requestDateString, "12:00", "14:30");
        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, userResourceId, requestDateString);

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resourceId);
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);
        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, resourceId, requestDateString);
        final List<Map<String, String>> resourceTimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotForWeeklyRecurringAvailability(serviceDuration, requestDateString);
        final List<Map<String, String>> resourceTimeSlotsNoUnavailableDates = availabilityTimeSlotsFlows.getExpectedTimeslotWithAvailabilityAndUnavailableDay(resourceTimeSlots, requestDateTime.toLocalDate());

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(resourceTimeSlotsNoUnavailableDates.size()))
                .body("", is(resourceTimeSlotsNoUnavailableDates));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6591")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingAppointmentCreated() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds * 3;
        final LocalDate requestDate = LocalDate.now().plusYears(1);
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDate);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));

        final String resource1Id = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resource1Id);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resource1Id, requestDateString, "09:00", "13:00");

        final String resource2Id = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resource2Id);
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resource2Id);

        final List<Map<String, String>> allAvailableTimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotForWeeklyRecurringAvailability(serviceDuration, requestDateString);
        appointmentsFlow.createAppointmentByGivenTime(organizationId, locationId, null, serviceId, requestDate.atTime(LocalTime.parse("09:00")));

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(allAvailableTimeSlots.size()))
                .body("", is(allAvailableTimeSlots));

        appointmentsFlow.createAppointmentByGivenTime(organizationId, locationId, null, serviceId, requestDate.atTime(LocalTime.parse("09:00")));
        final List<Map<String, String>> allAvailableTimeSlotsWithMissingSlot = availabilityTimeSlotsFlows.getExpectedTimeslotWithAvailabilityAndUnavailableDateTime(allAvailableTimeSlots, requestDate, "09:00");

        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/getListOfAvailableTimeSlots.json"))
                .body("size()", is(allAvailableTimeSlotsWithMissingSlot.size()))
                .body(is(allAvailableTimeSlotsWithMissingSlot));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6593")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingAppointmentCreatedOnlyFixedAvailability() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds * 3;
        final LocalDate requestDate = LocalDate.now().plusYears(1);
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDate);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resourceId);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, requestDateString, "09:00", "13:00");

        appointmentsFlow.createAppointmentByGivenTime(organizationId, locationId, null, serviceId, requestDate.atTime(LocalTime.parse("09:00")));
        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(0));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6594")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedHavingAppointmentCreatedOnlyRecurringAvailability() {
        final Role role = getRandomRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds * 3;
        final LocalDate requestDate = LocalDate.now().plusYears(1);
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDate);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resourceId);
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);

        final List<Map<String, String>> allAvailableTimeSlots = availabilityTimeSlotsFlows.getExpectedTimeslotForWeeklyRecurringAvailability(serviceDuration, requestDateString);
        appointmentsFlow.createAppointmentByGivenTime(organizationId, locationId, null, serviceId, requestDate.atTime(LocalTime.parse("09:00")));
        final List<Map<String, String>> allAvailableTimeSlotsWithMissingSlot = availabilityTimeSlotsFlows.getExpectedTimeslotWithAvailabilityAndUnavailableDateTime(allAvailableTimeSlots, requestDate, "09:00");

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(allAvailableTimeSlotsWithMissingSlot.size()))
                .body("", is(allAvailableTimeSlotsWithMissingSlot));
    }

    @Xray(requirement = "PEG-5841", test = "PEG-6596")
    @Test
    public void seeAvailableTimeSlotsNoResourceSelectedNoNoticeTimeLeft() {
        final Role role = getRandomRole();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");

        int serviceDuration = oneHourInSeconds / 4;
        final LocalDate requestDate = LocalDate.now();
        final String requestDateString = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(requestDate);

        final JSONObject service = serviceFlows.createServiceByDuration(organizationId, serviceDuration);
        final String serviceId = service.getString("id");
        serviceFlows.linkLocationsToService(organizationId, serviceId, Collections.singletonList(locationId));

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        serviceFlows.linkServiceToResource(organizationId, locationId, serviceId, resourceId);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, requestDateString, LocalTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm")), LocalTime.now().plusMinutes(60).format(DateTimeFormatter.ofPattern("HH:mm")));

        final JSONObject body = new GetAvailableTimeSlotsListBody().bodyBuilder(null);
        AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(token, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(0));
    }

    @Xray(requirement = "PEG-7030", test = "PEG-7158")
    @Test
    public void checkSortingOfAvailableTimeSlots() {
        final Role role = getRandomOrganizationRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");
        final String resourceId = resourceFlows.getResourceIdFromUserId(organizationId, userId);
        final String newServiceId = serviceFlows.createService(organizationId).getString("id");
        locationFlows.linkUnlinkServicesToLocation(organizationId, locationId, Collections.singletonList(newServiceId), LINK);
        userFlows.linkUnlinkUserToLocationService(organizationId, userId, locationId, newServiceId, LINK);
        final DayOfWeek randomDayOfWeek = DayOfWeek.of(TestUtils.getRandomInt(1, DayOfWeek.values().length));
        final JSONArray timeSlots = new JSONArray();
        final JSONObject firstTimeSlot = new JSONObject();
        firstTimeSlot.put(FROM, "15:00");
        firstTimeSlot.put(TO, "15:30");
        timeSlots.put(firstTimeSlot);
        final JSONObject secondTimeSlot = new JSONObject();
        secondTimeSlot.put(FROM, "14:00");
        secondTimeSlot.put(TO, "14:30");
        timeSlots.put(secondTimeSlot);

        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, locationId, resourceId, randomDayOfWeek, timeSlots);
        final JSONObject fixedAvailabilityBody = new FixedAvailabilityUpsertBody().bodyBuilder(resourceId);
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, fixedAvailabilityBody);
        final JSONObject timeSlotsBody = new GetAvailableTimeSlotsListBody().bodyBuilder(resourceId);
        final Response response = AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(userToken, organizationId, locationId,
                newServiceId, timeSlotsBody);
        response.then().statusCode(SC_OK);

        final List<String> fromDates = response.then().extract().path("from");
        final List<String> toDates = response.then().extract().path("to");

        response
                .then()
                .body("from.sort()", is(fromDates))
                .body("to.sort()", is(toDates));
    }


}



