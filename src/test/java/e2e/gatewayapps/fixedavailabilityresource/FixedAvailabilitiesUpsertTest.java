package e2e.gatewayapps.fixedavailabilityresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.fixedavailabilitiesresource.FixedAvailabilitiesHelper;
import helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody;
import helpers.flows.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.http.HttpStatus.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;
import static helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilityUpsertBody.*;

import static configuration.Role.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class FixedAvailabilitiesUpsertTest extends BaseTest {

    private JSONObject organizationAndUsers;
    private String organizationId;
    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private RecurringAvailabilitiesFlows recurringAvailabilitiesFlows;
    private FixedAvailabilitiesFlows fixedAvailabilitiesFlows;
    private UserFlows userFlows;
    private ResourceFlows resourceFlows;
    private FixedAvailabilityUpsertBody fixedAvailabilityUpsertBody;
    private String locationId;


    @BeforeClass(alwaysRun = true)
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        recurringAvailabilitiesFlows = new RecurringAvailabilitiesFlows();
        fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();
        userFlows = new UserFlows();
        resourceFlows = new ResourceFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        fixedAvailabilityUpsertBody = new FixedAvailabilityUpsertBody();
        locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

    }

    @Xray(test = "PEG-5269", requirement = "PEG-4585")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void upsertFixedAvailabilities(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot_1 = new JSONObject();
        timeSlot_1.put(FROM, "10:00");
        timeSlot_1.put(TO, "11:00");
        final JSONObject timeSlot_2 = new JSONObject();
        timeSlot_2.put(FROM, "11:01");
        timeSlot_2.put(TO, "14:00");
        timeSlots.put(timeSlot_1);
        timeSlots.put(timeSlot_2);

        fixedAvailabilityCreateBody.put(TIME_SLOTS, timeSlots);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId", hasItem(organizationId))
                .body("locationId", hasItem(locationId))
                .body("resourceId", hasItem(resourceId));
    }

    @Xray(test = "PEG-5294", requirement = "PEG-4585")
    @Test
    public void upsertFixedAvailabilityUnsupportedRole() {
        final String staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject fixedAvailabilityCreationBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(staffToken, organizationId, locationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-5295", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOverlappingWithRecurring() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(resourceId));
    }

    @Xray(test = "PEG-5353", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityAndUnavailableDayOnSameResourceDifferentLocation() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(locationId, otherLocationId)).getString("id");

        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));

        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, resourceId, date);

        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, otherLocationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(otherLocationId))
                .body("resourceId[0]", is(resourceId));
    }

    @Xray(test = "PEG-5296", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityInThePast() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final String lastYearDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().minusYears(1));
        fixedAvailabilityCreateBody.put(DATE, lastYearDate);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Xray(test = "PEG-5297", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOverlappingWithOtherLocation() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final List<String> locationIds = Arrays.asList(locationId, locationFlows.createLocation(organizationId).getString("id"));
        final String resourceId = resourceFlows.createActiveResource(organizationId, locationIds).getString("id");
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationIds.get(1), resourceId);
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("TIME_SLOT_OVERLAP_DETECTED"))
                .body("availabilityConflicts[0].recurringAvailabilities[0].organizationId", is(organizationId))
                .body("availabilityConflicts[0].recurringAvailabilities[0].resourceId", is(resourceId))
                .body("availabilityConflicts[0].recurringAvailabilities[0].locationId", is(locationIds.get(1)));
    }

    @Xray(test = "PEG-5298", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOverlapWithFixed() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final List<String> locationIds = Arrays.asList(locationId, locationFlows.createLocation(organizationId).getString("id"));
        final String resourceId = resourceFlows.createActiveResource(organizationId, locationIds).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationIds.get(1), resourceId, date);

        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        fixedAvailabilityCreateBody.put(DATE, date);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("TIME_SLOT_OVERLAP_DETECTED"))
                .body("availabilityConflicts[0].fixedAvailabilities[0].organizationId", is(organizationId))
                .body("availabilityConflicts[0].fixedAvailabilities[0].resourceId", is(resourceId))
                .body("availabilityConflicts[0].fixedAvailabilities[0].locationId", is(locationIds.get(1)));
    }

    @Xray(test = "PEG-5300", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityWithWrongFormattedDate() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDateTime.now().plusYears(1));
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        fixedAvailabilityCreateBody.put(DATE, date);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-5301", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOverlappingTimeSlots() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot_1 = new JSONObject();
        timeSlot_1.put(FROM, "10:00");
        timeSlot_1.put(TO, "19:00");
        final JSONObject timeSlot_2 = new JSONObject();
        timeSlot_2.put(FROM, "11:00");
        timeSlot_2.put(TO, "14:00");
        timeSlots.put(timeSlot_1);
        timeSlots.put(timeSlot_2);
        fixedAvailabilityCreateBody.put(TIME_SLOTS, timeSlots);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5351", requirement = "PEG-4585")
    @Test
    public void stickingBoundariesFixedAvailability() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot_1 = new JSONObject();
        timeSlot_1.put(FROM, "10:00");
        timeSlot_1.put(TO, "11:00");
        final JSONObject timeSlot_2 = new JSONObject();
        timeSlot_2.put(FROM, "11:00");
        timeSlot_2.put(TO, "14:00");
        timeSlots.put(timeSlot_1);
        timeSlots.put(timeSlot_2);
        fixedAvailabilityCreateBody.put(TIME_SLOTS, timeSlots);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(test = "PEG-5302", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOtherOrganizationResourceId() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final JSONObject otherOrganizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String otherOrganizationId = otherOrganizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationLocationId = otherOrganizationWithUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = resourceFlows.createActiveResource(otherOrganizationId, Collections.singletonList(otherOrganizationLocationId))
                .getString("id");
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-5352", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnUnavailableDate() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");

        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, resourceId, date);

        final JSONObject fixedAvailabilityCreationBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        fixedAvailabilityCreationBody.put(DATE, date);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(resourceId));
    }

    @Xray(test = "PEG-5303", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilitiesOnOtherOrganizationLocation() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(organizationAndUsers.getJSONObject(ADMIN.name()).getString("id"));

        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Xray(test = "PEG-5319", requirement = "PEG-4585")
    @Test
    public void editFixedAvailabilities() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));

        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);

        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(resourceId));
    }

    @Xray(test = "PEG-5320", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnInvitedUser() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");

        final String invitedUserId = DBHelper.getUserIdByEmail(userFlows.inviteUser(organizationId, ADMIN, Collections.singletonList(locationId)).getString("email"));
        final String invitedUserResourceID = DBHelper.getEmployeeResourceIdByUserId(invitedUserId);

        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(invitedUserResourceID);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(invitedUserResourceID));
    }

    @Xray(test = "PEG-5321", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnInactiveResource() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String inactiveResourceId = resourceFlows.createInactiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String inactiveUser = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(locationId)).getString("id");
        final String inactiveUserResourceId = DBHelper.getEmployeeResourceIdByUserId(inactiveUser);
        userFlows.inactivateUserById(organizationId, inactiveUser);


        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(inactiveResourceId);
        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(inactiveResourceId));

        fixedAvailabilityCreateBody.put(RESOURCE_ID, inactiveUserResourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(inactiveUserResourceId));
    }

    @Xray(test = "PEG-5322", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnPausedOrganization() {
        final Role randomRole = getRandomInviterRole();
        final JSONObject pausedOrganization = organizationFlows.createPausedOrganizationWithAllUsers();
        final String pausedOrganizationId = pausedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = pausedOrganization.getJSONObject("LOCATION").getString("id");
        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(pausedOrganization.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : pausedOrganization.getJSONObject(randomRole.name()).getString("token");

        FixedAvailabilitiesHelper.createFixedAvailability(token, pausedOrganizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(pausedOrganizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(resourceId));
    }

    @Xray(test = "PEG-5323", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnBlockedOrganization() {
        final Role randomRole = getRandomInviterRole();
        final JSONObject blockedOrganizationAndUsers = organizationFlows.createBlockedOrganizationWithAllUsers();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : blockedOrganizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String blockedOrganizationId = blockedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = blockedOrganizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(blockedOrganizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"));
        final JSONObject fixedAvailabilityCreateBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.createFixedAvailability(token, blockedOrganizationId, locationId, fixedAvailabilityCreateBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(blockedOrganizationId))
                .body("locationId[0]", is(locationId))
                .body("resourceId[0]", is(resourceId));
    }

    @Xray(test = "PEG-5324", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnUnpublishedOrganization() {
        final Role randomRole = getRandomInviterRole();
        final JSONObject unpublishedOrganizationAndUsers = organizationFlows.createUnpublishedOrganizationWithAllUsers();
        final String token = unpublishedOrganizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String unpublishedOrganizationId = unpublishedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String unpublishedOrganizationLocationId = unpublishedOrganizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String unpublishedOrganizationResourceId = resourceFlows.createActiveResource(unpublishedOrganizationId, Collections.singletonList(unpublishedOrganizationLocationId))
                .getString("id");

        final JSONObject fixedAvailabilityCreationBody = fixedAvailabilityUpsertBody.bodyBuilder(unpublishedOrganizationResourceId);
        FixedAvailabilitiesHelper.createFixedAvailability(token, unpublishedOrganizationId, unpublishedOrganizationLocationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(unpublishedOrganizationId))
                .body("locationId[0]", is(unpublishedOrganizationLocationId))
                .body("resourceId[0]", is(unpublishedOrganizationResourceId));

    }

    @Xray(test = "PEG-5325", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnDeletedOrganization() {
        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(organizationAndUsers.getJSONObject(OWNER.name()).getString("id"));
        organizationFlows.deleteOrganization(organizationId);

        final JSONObject fixedAvailabilityCreationBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, locationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-5326", requirement = "PEG-4585")
    @Test
    public void differentTimeZonLLocationOverlap() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String yerevanLocationId = locationFlows.createLocationInTimezone(organizationId, "Asia/Yerevan").getString("id");
        final String qatarLocationId = locationFlows.createLocationInTimezone(organizationId, "Asia/Qatar").getString("id");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(yerevanLocationId, qatarLocationId)).getString("id");

        final JSONObject fixedAvailabilityCreationBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "10:00");
        timeSlot.put(TO, "10:30");
        timeSlots.put(timeSlot);
        fixedAvailabilityCreationBody.put(TIME_SLOTS, timeSlots);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, yerevanLocationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(yerevanLocationId))
                .body("resourceId[0]", is(resourceId));

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, qatarLocationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId[0]", is(organizationId))
                .body("locationId[0]", is(qatarLocationId))
                .body("resourceId[0]", is(resourceId));

    }

    @Xray(test = "PEG-5337", requirement = "PEG-4585")
    @Test
    public void fixedAvailabilityOnOtherLocationByLocationAdmin() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(locationId, newLocationId)).getString("id");

        final JSONObject fixedAvailabilityCreationBody = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final String locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        FixedAvailabilitiesHelper.createFixedAvailability(locationAdminToken, organizationId, newLocationId, fixedAvailabilityCreationBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-5338", requirement = "PEG-4585")
    @Test
    public void mismatchFromToFixedAvailabilities() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final JSONObject fixedAvailabilityCreation = fixedAvailabilityUpsertBody.bodyBuilder(resourceId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "18:00");
        timeSlot.put(TO, "10:00");
        timeSlots.put(timeSlot);
        fixedAvailabilityCreation.put(TIME_SLOTS, timeSlots);

        FixedAvailabilitiesHelper.createFixedAvailability(token, organizationId, locationId, fixedAvailabilityCreation)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }


}
