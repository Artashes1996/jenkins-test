package e2e.gatewayapps.fixedavailabilityresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.fixedavailabilitiesresource.FixedAvailabilitiesHelper;
import helpers.appsapi.fixedavailabilitiesresource.payloads.FixedAvailabilitySearchBody;
import helpers.appsapi.recurringavailabilitiesresource.RecurringAvailabilitiesHelper;
import helpers.flows.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import utils.Xray;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import static helpers.appsapi.recurringavailabilitiesresource.payloads.CreateRecurringRequestBody.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.commons.ToggleAction.*;

public class SearchFixedAvailabilityTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private LocationFlows locationFlows;
    private UserFlows userFlows;
    private ResourceFlows resourceFlows;
    private FixedAvailabilitiesFlows fixedAvailabilitiesFlows;
    private FixedAvailabilitySearchBody fixedAvailabilitySearchBody;

    private JSONObject organizationWithUsers;
    private String organizationId;
    private String locationId;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        locationFlows = new LocationFlows();
        userFlows = new UserFlows();
        resourceFlows = new ResourceFlows();
        fixedAvailabilitiesFlows = new FixedAvailabilitiesFlows();
        fixedAvailabilitySearchBody = new FixedAvailabilitySearchBody();

        organizationWithUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationWithUsers.getJSONObject("ORGANIZATION").getString("id");
        locationId = organizationWithUsers.getJSONObject("LOCATION").getString("id");
    }

    @Xray(test = "PEG-5403", requirement = "PEG-4527")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void searchFixedAvailabilitiesWithSupportedUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date1 = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        final String date2 = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1).minusDays(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date1);
        final String from = "01:23";
        final String to = "12:34";
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, date2, from, to);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("size()", is(2))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("date", containsInAnyOrder(date1, date2))
                .body("fromTime", hasItem(from))
                .body("toTime", hasItem(to));
    }


    @Xray(test = "PEG-5439", requirement = "PEG-4527")
    @Test(dataProvider = "inviters", dataProviderClass = RoleDataProvider.class)
    public void searchEmployeeTypeResourceFixedAvailability(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationWithUsers.getJSONObject(role.name()).getString("token");

        final Role randomRole = getRandomOrganizationRole();

        final String userId = userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId)).getString("id");
        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(userId);

        final String date1 = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        final String date2 = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1).minusDays(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date1);
        final String from = "12:34";
        final String to = "23:45";
        fixedAvailabilitiesFlows.createFixedAvailability(organizationId, locationId, resourceId, date2, from, to);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("size()", is(2))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("date", containsInAnyOrder(date1, date2))
                .body("fromTime", hasItem(from))
                .body("toTime", hasItem(to));
    }

    @Xray(test = "PEG-5404", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesUnSUPPORT_TOKENortedUser() {
        final String staffToken = organizationWithUsers.getJSONObject(STAFF.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(staffToken, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Xray(test = "PEG-5406", requirement = "PEG-4527")
    @Test
    public void searchOtherOrganizationResourceFixedAvailability() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String otherOrganizationId = organizationFlows.createBlockedOrganizationWithOwner()
                .getJSONObject("ORGANIZATION").getString("id");
        final String locationId = locationFlows.createLocation(otherOrganizationId).getString("id");

        final String resourceId = resourceFlows.createActiveResource(otherOrganizationId, Collections.singletonList(locationId))
                .getString("id");
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5407", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesOnOtherLocationResource() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(otherLocationId))
                .getString("id");

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5408", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilityOnUnavailableDate() {
        final Role randomRole = getRandomAdminRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId))
                .getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));

        fixedAvailabilitiesFlows.makeDayUnavailable(organizationId, locationId, resourceId, date);
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("size()", is(1))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)))
                .body("date[0]", is(date))
                .body("fromTime[0]", is("00:00"))
                .body("toTime[0]", is("23:59"))
                .body("available[0]", is(false));

    }

    @Xray(test = "PEG-5410", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesInThePast() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        final String searchDateTo = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().minusYears(1));
        final String searchDateFrom = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().minusYears(2));
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        searchBody.put(FROM, searchDateFrom);
        searchBody.put(TO, searchDateTo);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5411", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesMismatchedToFrom() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        final String searchDateTo = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now());
        final String searchDateFrom = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        searchBody.put(FROM, searchDateFrom);
        searchBody.put(TO, searchDateTo);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-5411", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesWrongFormattedDates() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        final String searchDateFrom = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDateTime.now());
        final String searchDateTo = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDateTime.now().plusYears(1));
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        searchBody.put(FROM, searchDateFrom);
        searchBody.put(TO, searchDateTo);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Xray(test = "PEG-5413", requirement = "PEG-4527")
    @Test
    public void searchInDateWithOnlyRecurringAvailabilities() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final RecurringAvailabilitiesFlows recurringAvailabilitiesFlows = new RecurringAvailabilitiesFlows();
        recurringAvailabilitiesFlows.createRecurringAvailabilityForWeek(organizationId, locationId, resourceId);
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5441", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesWithEmptyBody() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, new JSONObject())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }


    @Xray(test = "PEG-5405", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilityOnOtherLocationByLocationAdmin() {
        final String locationAdminToken = organizationWithUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");

        final String otherLocationId = locationFlows.createLocation(organizationId).getString("id");

        final String resourceId = resourceFlows.createActiveResource(organizationId, Collections.singletonList(otherLocationId))
                .getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, otherLocationId, resourceId, date);
        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.searchFixedAvailabilities(locationAdminToken, organizationId, otherLocationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN);
    }


    @Xray(requirement = "PEG-4732", test = "PEG-5366")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void removeLocationFromUserAndCheckAvailability(Role role) {
        final String locationIdToUnlinkUserFrom = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Arrays.asList(locationId, locationIdToUnlinkUserFrom));

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(user.getString("id"));
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationIdToUnlinkUserFrom, resourceId, date);
        userFlows.linkUnlinkLocationsToUser(organizationId, user.getString("id"), Collections.singletonList(locationIdToUnlinkUserFrom), UNLINK);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.searchFixedAvailabilities(SUPPORT_TOKEN, organizationId, locationIdToUnlinkUserFrom, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5367")
    @Test
    public void removeLocationFromResourceAndCheckAvailability() {
        final String locationIdToUnlinkFromResource = locationFlows.createLocation(organizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(organizationId, Arrays.asList(locationId, locationIdToUnlinkFromResource)).getString("id");

        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationIdToUnlinkFromResource, resourceId, date);
        resourceFlows.linkUnlinkLocationToResource(organizationId, locationIdToUnlinkFromResource, resourceId, UNLINK);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.searchFixedAvailabilities(SUPPORT_TOKEN, organizationId, locationIdToUnlinkFromResource, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5368")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void deleteUserAndCheckAvailability(Role role) {

        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        userFlows.deleteUser(organizationId, userId);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.searchFixedAvailabilities(SUPPORT_TOKEN, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5369")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void changeOnlyRoleAndCheckAvailability(Role role) {
        final String userId = userFlows.createUser(organizationId, role, Collections.singletonList(locationId)).getString("id");

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(userId);
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "19:00");
        timeSlots.put(timeSlot);

        final Role roleToChange = role.equals(Role.STAFF) ? Role.ADMIN : Role.STAFF;
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);
        userFlows.changeRoleOfUser(organizationId, userId, roleToChange);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.searchFixedAvailabilities(SUPPORT_TOKEN, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("size()", is(1))
                .body("resourceId.get(0)", is(resourceId));
    }

    @Xray(requirement = "PEG-4732", test = "PEG-5370")
    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void changeLocationAndCheckAvailability(Role role) {
        final String locationIdToUnlinkUserFrom = locationFlows.createLocation(organizationId).getString("id");
        final JSONObject user = userFlows.createUser(organizationId, role, Arrays.asList(locationId, locationIdToUnlinkUserFrom));

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(user.getString("id"));
        final JSONArray timeSlots = new JSONArray();
        final JSONObject timeSlot = new JSONObject();
        timeSlot.put(FROM, "09:00");
        timeSlot.put(TO, "19:00");
        timeSlots.put(timeSlot);

        final Role roleToChange = role.equals(Role.STAFF) ? Role.ADMIN : Role.STAFF;
        final String date1 = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusMonths(1));
        final String date2 = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusMonths(2));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date1);
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationIdToUnlinkUserFrom, resourceId, date2);
        userFlows.changeRoleAndLocationOfUser(organizationId, user.getString("id"), roleToChange, Collections.singletonList(locationId));

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);
        FixedAvailabilitiesHelper.searchFixedAvailabilities(SUPPORT_TOKEN, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body("", hasSize(1))
                .body("resourceId", hasItem(resourceId));

        RecurringAvailabilitiesHelper.getRecurringAvailability(SUPPORT_TOKEN, organizationId, locationIdToUnlinkUserFrom, resourceId)
                .then()
                .statusCode(SC_OK)
                .body("isEmpty()", is(true));
    }

    @Xray(test = "PEG-5444", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesDeletedOrganization() {
        final JSONObject deletedOrganization = organizationFlows.createAndPublishOrganizationWithOwner();
        final String deletedOrganizationId = deletedOrganization.getJSONObject("ORGANIZATION").getString("id");
        final String deletedOrganizationLocationId = locationFlows.createLocation(deletedOrganizationId).getString("id");
        final String resourceId = resourceFlows.createActiveResource(deletedOrganizationId, Collections.singletonList(deletedOrganizationLocationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));
        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(deletedOrganizationId, deletedOrganizationLocationId, resourceId, date);

        organizationFlows.deleteOrganization(deletedOrganizationId);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(SUPPORT_TOKEN, deletedOrganizationId, deletedOrganizationLocationId, searchBody)
                .then()
                .statusCode(SC_OK).body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId", everyItem(is(deletedOrganizationId)))
                .body("locationId", everyItem(is(deletedOrganizationLocationId)))
                .body("resourceId", everyItem(is(resourceId)));
    }

    @Xray(test = "PEG-5442", requirement = "PEG-4527")
    @Test
    public void searchFixedAvailabilitiesOnInactiveOtherTypeResource() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String resourceId = resourceFlows.createInactiveResource(organizationId, Collections.singletonList(locationId)).getString("id");
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));

        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK).body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)));
    }

    @Xray(test = "PEG-5443", requirement = "PEG-4725")
    @Test
    public void searchFixedAvailabilitiesOnEmployeeTypeResource() {
        final Role randomRole = getRandomInviterRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN
                : organizationWithUsers.getJSONObject(randomRole.name()).getString("token");
        final String inactiveUserId = userFlows.createUser(organizationId, getRandomOrganizationRole(), Collections.singletonList(locationId)).getString("id");
        userFlows.inactivateUserById(organizationId, inactiveUserId);

        final String resourceId = DBHelper.getEmployeeResourceIdByUserId(inactiveUserId);
        final String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now().plusYears(1));

        fixedAvailabilitiesFlows.createFixedAvailabilityAllDay(organizationId, locationId, resourceId, date);

        final JSONObject searchBody = fixedAvailabilitySearchBody.bodyBuilder(resourceId);

        FixedAvailabilitiesHelper.searchFixedAvailabilities(token, organizationId, locationId, searchBody)
                .then()
                .statusCode(SC_OK).body(matchesJsonSchemaInClasspath("schemas/fixedAvailabilityUpsert.json"))
                .body("organizationId", everyItem(is(organizationId)))
                .body("locationId", everyItem(is(locationId)))
                .body("resourceId", everyItem(is(resourceId)));
    }

}
