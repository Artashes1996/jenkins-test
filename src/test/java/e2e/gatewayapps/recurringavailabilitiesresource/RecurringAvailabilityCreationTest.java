package e2e.gatewayapps.recurringavailabilitiesresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.fixedavailabilitiesresource.FixedAvailabilitiesHelper;
import helpers.appsapi.recurringavailabilitiesresource.RecurringAvailabilitiesHelper;
import helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody;
import helpers.flows.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.Xray;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.UUID;

import static configuration.Role.*;
import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.LINK;

public class RecurringAvailabilityCreationTest extends BaseTest {

    private String organizationId;
    private String locationId;
    private String resourceId;

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private ResourceFlows resourceFlows;
    private UserFlows userFlows;
    private RecurringAvailabilitiesFlows recurringAvailabilitiesFlows;

    private JSONObject organizationAndUsersObject;
    private CreateRecurringRequestBody createRecurringRequestBody;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        resourceFlows = new ResourceFlows();
        userFlows = new UserFlows();
        recurringAvailabilitiesFlows = new RecurringAvailabilitiesFlows();
        createRecurringRequestBody = new CreateRecurringRequestBody();
        organizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4969")
    @Test
    public void recurringAvailabilityBulkUsingFakeOrganizationIdBySupportRole() {
        final String fakeOrganizationId = UUID.randomUUID().toString();
        final JSONObject bodyBuilder = createRecurringRequestBody.bodyBuilder(resourceId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, fakeOrganizationId, locationId, bodyBuilder)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4970")
    @Test
    public void recurringAvailabilityBulkUsingFakeLocationIdBySupportedRoles() {
        final String fakeLocationId = UUID.randomUUID().toString();
        final Role role = getRandomOrganizationAdminRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject bodyBuilder = createRecurringRequestBody.bodyBuilder(resourceId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, fakeLocationId, bodyBuilder)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4974")
    @Test
    public void recurringAvailabilityBulkUsingFakeLocationIdByRolesWithLocation() {
        final String fakeLocationId = UUID.randomUUID().toString();
        final String userToken = organizationAndUsersObject.getJSONObject(Role.getRandomRolesWithLocation().name()).getString("token");
        final JSONObject bodyBuilder = createRecurringRequestBody.bodyBuilder(resourceId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, fakeLocationId, bodyBuilder)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4975")
    @Test
    public void recurringAvailabilityBulkWithoutDayOfWeekKeyBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4976")
    @Test
    public void recurringAvailabilityBulWithInvalidDayOfWeekBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(DAY_OFF_WEEK, "INVALID");
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4978")
    @Test
    public void recurringAvailabilityBulkWithFakeResourceIdBySupportedRoles() {
        final String fakeResourceId = UUID.randomUUID().toString();
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        createRecurringBody.put(RESOURCE_ID, fakeResourceId);
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4979")
    @Test
    public void recurringAvailabilityBulkWithoutResourceIdKeyBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4980")
    @Test
    public void recurringAvailabilityBulkWithoutTimeSlotsBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-4981")
    @Test
    public void recurringAvailabilityBulkWithEmptyFromToBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray.put(new JSONObject().put("from", "").put("to", "")));
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5005")
    @Test
    public void recurringAvailabilityInCaseOfEndDateLaterThanStartDateBySupportedRoles() {
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        timeSlotsArray.put(new JSONObject().put(FROM, "09:30").put(TO, "09:00"));
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5019")
    @Test
    public void recurringAvailabilityInCaseOfSameStartDateAndEndDateBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        timeSlotsArray.put(new JSONObject().put(FROM, "09:00").put(TO, "09:00"));
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5020")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void recurringAvailabilityBulkBySupportedRoles(Role role) {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationId, newResourceId, LINK);
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject bodyBuilder = createRecurringRequestBody.bodyBuilder(newResourceId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, bodyBuilder)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5021")
    @Test
    public void createRecurringAvailabilityInCaseOfInnerOverlapBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        timeSlotsArray.put(new JSONObject().put(FROM, "09:00").put(TO, "09:30"));
        timeSlotsArray.put(new JSONObject().put(FROM, "09:15").put(TO, "09:45"));
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5022")
    @Test
    public void timesOverlapWithAnotherLocationRecurringAvailability() {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        final Role role = getRandomOrganizationAdminRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject availabilityCreationRequest = createRecurringRequestBody.bodyBuilder(newResourceId);
        final String firstLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String secondLocationId = locationFlows.createLocation(organizationId).getString("id");
        if (role.equals(LOCATION_ADMIN)) {
            final String locationAdminId = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("id");
            userFlows.linkUnlinkLocationToUser(organizationId, locationAdminId, secondLocationId, LINK);
        }
        resourceFlows.linkUnlinkLocationToResource(organizationId, firstLocationId, newResourceId, LINK);
        resourceFlows.linkUnlinkLocationToResource(organizationId, secondLocationId, newResourceId, LINK);
        RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, organizationId, firstLocationId, availabilityCreationRequest)
                .then()
                .statusCode(SC_OK);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, secondLocationId, availabilityCreationRequest)
                .then()
                .statusCode(SC_PRECONDITION_FAILED)
                .body("type", is("TIME_SLOT_OVERLAP_DETECTED"));
    }

    // TODO investigate this failure
    @Xray(requirement = "PEG-4832", test = "PEG-5023")
    @Test(enabled = false)
    public void timesOverlapWithAnotherLocationFixedAvailability() {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        final Role role = getRandomInviterRole();

        final String firstLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String secondLocationId = locationFlows.createLocation(organizationId).getString("id");
        if (role.equals(LOCATION_ADMIN)) {
            final String locationAdminId = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("id");
            userFlows.linkUnlinkLocationToUser(organizationId, locationAdminId, secondLocationId, LINK);
        }
        resourceFlows.linkUnlinkLocationToResource(organizationId, firstLocationId, newResourceId, LINK);
        resourceFlows.linkUnlinkLocationToResource(organizationId, secondLocationId, newResourceId, LINK);

        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
//      TODO move this to bodyBuilder
        final JSONObject createFixedAvailabilityBody = new JSONObject();
        final JSONArray timeSlotsArrayForFixedAvailability = new JSONArray();
        createFixedAvailabilityBody.put(RESOURCE_ID, newResourceId);
        createFixedAvailabilityBody.put("date", "2021-12-31");
        timeSlotsArrayForFixedAvailability.put(new JSONObject().put(FROM, "10:45").put(TO, "11:15"));
        createFixedAvailabilityBody.put(TIME_SLOTS, timeSlotsArrayForFixedAvailability);

        FixedAvailabilitiesHelper.createFixedAvailability(userToken, organizationId, firstLocationId, createFixedAvailabilityBody)
                .then()
                .statusCode(SC_OK);
        final JSONObject createRecurringAvailabilityBody = new JSONObject();
        final JSONArray timeSlotsArrayForRecurringAvailability = new JSONArray();
        createRecurringAvailabilityBody.put(RESOURCE_ID, newResourceId);
        createRecurringAvailabilityBody.put(DAY_OFF_WEEK, "FRIDAY");
        timeSlotsArrayForRecurringAvailability.put(new JSONObject().put(FROM, "10:30").put(TO, "11:00"));
        createRecurringAvailabilityBody.put(TIME_SLOTS, timeSlotsArrayForRecurringAvailability);
        RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, organizationId, secondLocationId, createRecurringAvailabilityBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    // TODO investigate this failure
    @Xray(requirement = "PEG-4832", test = "PEG-5026")
    @Test(enabled = false)
    public void timesOverlapWithFixedAvailabilityDifferentTimezone() {
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        final Role role = getRandomInviterRole();
        final String firstLocationId = locationFlows.createLocationInTimezone(organizationId, "Asia/Yerevan").getString("id");
        final String secondLocationId = locationFlows.createLocationInTimezone(organizationId, "Asia/Yekaterinburg").getString("id");
        resourceFlows.linkUnlinkLocationToResource(organizationId, firstLocationId, newResourceId, LINK);
        resourceFlows.linkUnlinkLocationToResource(organizationId, secondLocationId, newResourceId, LINK);
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
//      TODO move this to bodyBuilder
        final JSONObject createFirstFixedAvailabilityBody = new JSONObject();
        final JSONArray firstTimeSlotsArray = new JSONArray();
        createFirstFixedAvailabilityBody.put(RESOURCE_ID, newResourceId);
        createFirstFixedAvailabilityBody.put("date", "2021-12-31");
        firstTimeSlotsArray.put(new JSONObject().put(FROM, "10:45").put(TO, "11:15"));
        createFirstFixedAvailabilityBody.put(TIME_SLOTS, firstTimeSlotsArray);

        FixedAvailabilitiesHelper.createFixedAvailability(SUPPORT_TOKEN, organizationId, firstLocationId,
                createFirstFixedAvailabilityBody);

        final JSONObject createRecurringAvailabilityBody = new JSONObject();
        final JSONArray secondTimeSlotsArray = new JSONArray();
        createRecurringAvailabilityBody.put(RESOURCE_ID, newResourceId);
        createRecurringAvailabilityBody.put(DAY_OFF_WEEK, "FRIDAY");
        secondTimeSlotsArray.put(new JSONObject().put(FROM, "11:45").put(TO, "12:00" + ""));
        createRecurringAvailabilityBody.put(TIME_SLOTS, secondTimeSlotsArray);

        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, secondLocationId, createRecurringAvailabilityBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5027")
    @Test
    public void timesOverlapWithRecurringAvailabilityDifferentTimezone() {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        final Role role = getRandomInviterRole();
        final String firstLocationId = locationFlows.createLocationInTimezone(organizationId, "Asia/Yerevan").getString("id");
        final String secondLocationId = locationFlows.createLocationInTimezone(organizationId, "Asia/Yekaterinburg").getString("id");

        if (role.equals(LOCATION_ADMIN)) {
            final String locationAdminId = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("id");
            userFlows.linkUnlinkLocationToUser(organizationId, locationAdminId, secondLocationId, LINK);
            final String locationAdminEmail = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("email");
            organizationAndUsersObject.getJSONObject(role.name())
                    .put("token", new AuthenticationFlowHelper().getTokenWithEmail(locationAdminEmail));
        }

        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        resourceFlows.linkUnlinkLocationToResource(organizationId, firstLocationId, newResourceId, LINK);
        resourceFlows.linkUnlinkLocationToResource(organizationId, secondLocationId, newResourceId, LINK);

        recurringAvailabilitiesFlows.createRecurringAvailability(organizationId, firstLocationId, newResourceId, DayOfWeek.FRIDAY, new JSONArray()
                .put(new JSONObject().put(FROM, "09:30").put(TO, "10:15"))
                .put(new JSONObject().put(FROM, "10:30").put(TO, "11:00")));

        final JSONObject createRecurringAvailabilityBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringAvailabilityBody.put(RESOURCE_ID, newResourceId);
        createRecurringAvailabilityBody.put(DAY_OFF_WEEK, "FRIDAY");
        timeSlotsArray.put(new JSONObject().put(FROM, "10:00").put(TO, "10:30"));
        createRecurringAvailabilityBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, secondLocationId, createRecurringAvailabilityBody)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }


    @Xray(requirement = "PEG-4832", test = "PEG-5061")
    @Test(dataProvider = "otherOrganizationUsers", dataProviderClass = RoleDataProvider.class)
    public void createRecurringAvailabilityUsingOtherOrganizationsUsers(Role role) {
        final JSONObject otherOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String userToken = otherOrganizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject bodyBuilder = createRecurringRequestBody.bodyBuilder(newResourceId);
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationId, newResourceId, LINK);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId, bodyBuilder)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    // TODO investigate failure
    @Xray(requirement = "PEG-4832", test = "PEG-5062")
    @Test(enabled = false)
    public void recurringAvailabilityBulkOverlapping() {
        final String newResourceId = resourceFlows.createActiveResource(organizationId, null).getString("id");
        final Role role = getRandomInviterRole();
        final JSONObject bodyBuilder = createRecurringRequestBody.bodyBuilder(newResourceId);
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationId, newResourceId, LINK);
        final String firstLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String secondLocationId = locationFlows.createLocation(organizationId).getString("id");
        if (role.equals(LOCATION_ADMIN)) {
            final String locationAdminId = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("id");
            userFlows.linkUnlinkLocationToUser(organizationId, locationAdminId, secondLocationId, LINK);
            final String locationAdminEmail = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("email");
            organizationAndUsersObject.getJSONObject(role.name())
                    .put("token", new AuthenticationFlowHelper().getTokenWithEmail(locationAdminEmail));
        }
        resourceFlows.linkUnlinkLocationToResource(organizationId, firstLocationId, newResourceId, LINK);
        resourceFlows.linkUnlinkLocationToResource(organizationId, secondLocationId, newResourceId, LINK);
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsersObject.getJSONObject(role.name()).getString("email"));
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, firstLocationId, bodyBuilder)
                .then()
                .statusCode(SC_OK);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, secondLocationId, bodyBuilder)
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5072")
    @Test
    public void recurringAvailabilityBulkInCaseOfDeletedOrganization() {
        final JSONObject deletedOrganizationAndUsersObject = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String deletedOrganizationId = deletedOrganizationAndUsersObject.getJSONObject("ORGANIZATION").getString("id");
        final String deletedLocationId = deletedOrganizationAndUsersObject.getJSONObject("LOCATION").getString("id");
        final String deletedResourceId = resourceFlows.createActiveResource(deletedOrganizationId, Collections.singletonList(deletedLocationId)).getString("id");
        organizationFlows.deleteOrganization(deletedOrganizationId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(SUPPORT_TOKEN, deletedOrganizationId, deletedLocationId, createRecurringRequestBody.bodyBuilder(deletedResourceId))
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5086")
    @Test
    public void createRecurringAvailabilityInCaseOfInnerOverlapStickingSlotsBySupportedRoles() {
        final Role role = getRandomInviterRole();
        final String userToken = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsersObject.getJSONObject(role.name()).getString("token");
        final JSONObject createRecurringBody = new JSONObject();
        final JSONArray timeSlotsArray = new JSONArray();
        createRecurringBody.put(RESOURCE_ID, resourceId);
        createRecurringBody.put(DAY_OFF_WEEK, "FRIDAY");
        timeSlotsArray.put(new JSONObject().put(FROM, "09:00").put(TO, "11:00"));
        timeSlotsArray.put(new JSONObject().put(FROM, "11:00").put(TO, "13:00"));
        createRecurringBody.put(TIME_SLOTS, timeSlotsArray);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, locationId, createRecurringBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("types[0]", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5087")
    @Test
    public void createRecurringAvailabilityWithLocationWhereNotSpecifiedLocationAdmin() {
        final String userToken = organizationAndUsersObject.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(newResourceId))
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5089")
    @Test
    public void createRecurringAvailabilityByStaff() {
        final String userToken = organizationAndUsersObject.getJSONObject(STAFF.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String newResourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(newLocationId)).getString("id");
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(newResourceId))
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("types[0]", is("FORBIDDEN_ACCESS"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5091")
    @Test
    public void createRecurringAvailabilityForPendingEmployeeStatus() {
        final String userToken = organizationAndUsersObject.getJSONObject(OWNER.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String userId = DBHelper.getUserIdByEmail(userFlows.inviteUser(organizationId, OWNER, Collections.singletonList(newLocationId)).getString("email"));
        final String newResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(newResourceId))
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5092")
    @Test
    public void createRecurringAvailabilityForAcceptedEmployeeStatus() {
        final String userToken = organizationAndUsersObject.getJSONObject(OWNER.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, OWNER, Collections.singletonList(newLocationId))
                .getString("id");
        final String newResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(newResourceId))
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5093")
    @Test
    public void createRecurringAvailabilityForInactiveEmployee() {
        final String userToken = organizationAndUsersObject.getJSONObject(OWNER.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, OWNER, Collections.singletonList(newLocationId))
                .getString("id");
        final String newResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        userFlows.inactivateUserById(organizationId, userId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(newResourceId))
                .then()
                .statusCode(SC_OK);
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5094")
    @Test
    public void createRecurringAvailabilityForDeletedEmployee() {
        final String userToken = organizationAndUsersObject.getJSONObject(OWNER.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final String userId = userFlows.createUser(organizationId, OWNER, Collections.singletonList(newLocationId))
                .getString("id");
        final String newResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        userFlows.deleteUser(organizationId, userId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(newResourceId))
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("types[0]", is("RESOURCE_NOT_FOUND"));
    }

    @Xray(requirement = "PEG-4832", test = "PEG-5094")
    @Test
    public void createRecurringAvailabilityForRestoredEmployee() {
        final String userToken = organizationAndUsersObject.getJSONObject(OWNER.name()).getString("token");
        final String newLocationId = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject userObject = userFlows.createUser(organizationId, OWNER, Collections.singletonList(newLocationId));
        final String userId = userObject.getString("id");
        userFlows.deleteUser(organizationId, userId);
        userFlows.requestRestore(organizationId, userObject);
        userFlows.restoreUser(organizationId, userObject);
        final String restoredResourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        RecurringAvailabilitiesHelper.createRecurringAvailability(userToken, organizationId, newLocationId,
                createRecurringRequestBody.bodyBuilder(restoredResourceId))
                .then()
                .statusCode(SC_OK);
    }
}
