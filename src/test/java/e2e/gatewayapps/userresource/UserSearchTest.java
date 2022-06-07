package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import e2e.gatewayapps.userresource.data.UserDataProvider;
import helpers.DBHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.appsapi.usersresource.UserHelper;
import helpers.appsapi.usersresource.payloads.UserSearchBody;
import helpers.flows.UserFlows;
import org.json.*;
import org.testng.annotations.*;
import utils.Xray;

import java.util.*;
import java.util.stream.Collectors;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static org.apache.http.HttpStatus.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;

public class UserSearchTest extends BaseTest {

    private List<JSONObject> usersWithPOC;
    private List<JSONObject> allUsers;

    private OrganizationFlows organizationFlows;
    private String organizationId;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;
    private UserFlows userFlows;
    private AuthenticationFlowHelper authenticationFlowHelper;
    private LocationFlows locationFlows;
    private List<String> locationIds;
    private JSONObject locationUsers;
    private JSONObject organizationAndUsers;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        userFlows = new UserFlows();
        authenticationFlowHelper = new AuthenticationFlowHelper();
        locationFlows = new LocationFlows();
        allUsers = new ArrayList<>();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        locationIds = new ArrayList<>();
        locationIds.add(organizationAndUsers.getJSONObject("LOCATION").getString("id"));

        locationUsers = new JSONObject();

        final JSONObject owner = organizationAndUsers.getJSONObject(OWNER.name());
        final JSONObject admin = organizationAndUsers.getJSONObject(ADMIN.name());
        final JSONObject locationAdmin = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name());
        final JSONObject staff = organizationAndUsers.getJSONObject(STAFF.name());

        usersWithPOC = new ArrayList<>();
        usersWithPOC.add(owner);
        usersWithPOC.add(admin);
        usersWithPOC.add(locationAdmin);
        usersWithPOC.add(staff);

        allUsers.addAll(usersWithPOC);

        final JSONObject inactiveUser = userFlows.createUserWithoutPOC(organizationId, OWNER, null);
        userFlows.inactivateUserById(organizationId, inactiveUser.getString("id"));
        inactiveUser.put("status", "INACTIVE");

        ownerToken = authenticationFlowHelper.getTokenWithEmail(owner.getString("email"));
        adminToken = authenticationFlowHelper.getTokenWithEmail(admin.getString("email"));
        locationAdminToken = authenticationFlowHelper.getTokenWithEmail(locationAdmin.getString("email"));
        staffToken = authenticationFlowHelper.getTokenWithEmail(staff.getString("email"));

        for (int i = 0; i < 5; i++) {
            final String physicalLocationId = locationFlows.createLocation(organizationId).getString("id");
            final String virtualLocationId = locationFlows.createLocation(organizationId).getString("id");
            locationIds.add(physicalLocationId);
            locationIds.add(virtualLocationId);
        }

        final List<JSONObject> noLocationUsers = new ArrayList<>();
        noLocationUsers.add(userFlows.createUserWithoutPOC(organizationId, OWNER, null));
        noLocationUsers.add(userFlows.createUserWithoutPOC(organizationId, ADMIN, null));
        noLocationUsers.add(inactiveUser);

        allUsers.addAll(noLocationUsers);

        locationUsers.put("no_location", noLocationUsers);

        final List<JSONObject> allLocationsUsers = new ArrayList<>();
        allLocationsUsers.add(userFlows.createUserWithoutPOC(organizationId, OWNER, locationIds));
        allLocationsUsers.add(userFlows.createUserWithoutPOC(organizationId, ADMIN, locationIds));
        allLocationsUsers.add(userFlows.createUserWithoutPOC(organizationId, LOCATION_ADMIN, locationIds));
        allLocationsUsers.add(userFlows.createUserWithoutPOC(organizationId, STAFF, locationIds));

        allUsers.addAll(allLocationsUsers);

        locationUsers.put("all_locations", allLocationsUsers);

        final JSONObject location0_2_4_User = userFlows.createUserWithoutPOC(organizationId, LOCATION_ADMIN, Arrays.asList(locationIds.get(0), locationIds.get(2), locationIds.get(4)));
        locationUsers.put(locationIds.get(2), Collections.singletonList(location0_2_4_User));

        final JSONObject deletedUser = userFlows.createUser(organizationId, ADMIN, Collections.singletonList(locationIds.get(4)));
        allUsers.add(0, deletedUser);
        userFlows.deleteUser(organizationId, deletedUser.getString("id"));
        locationUsers.put(locationIds.get(4), Arrays.asList(location0_2_4_User, deletedUser));

        final List<JSONObject> location_0_users = new ArrayList<>();
        location_0_users.add(organizationAndUsers.getJSONObject(OWNER.name()));
        location_0_users.add(organizationAndUsers.getJSONObject(ADMIN.name()));
        location_0_users.add(organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()));
        location_0_users.add(organizationAndUsers.getJSONObject(STAFF.name()));
        location_0_users.add(location0_2_4_User);

        locationUsers.put(locationIds.get(0), location_0_users);

        allUsers.add(location0_2_4_User);

        final JSONObject location1_3_User = userFlows.createUserWithoutPOC(organizationId, LOCATION_ADMIN, Arrays.asList(locationIds.get(1), locationIds.get(3)));

        locationUsers.put(locationIds.get(1), Collections.singletonList(location1_3_User));
        locationUsers.put(locationIds.get(3), Collections.singletonList(location1_3_User));

        allUsers.add(location1_3_User);
    }

    @Test(testName = "PEG-470", dataProvider = "validSize", dataProviderClass = UserDataProvider.class)
    public void checkUserList(Object size) {
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, size);
        searchBody.put(UserSearchBody.PAGINATION, body);

        UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(adminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(staffToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));
    }


    @Test(testName = "PEG-466", dataProvider = "validPage", dataProviderClass = UserDataProvider.class)
    public void checkUserListPage(Object page) {
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, page);
        body.put(UserSearchBody.SIZE, 1);
        searchBody.put(UserSearchBody.PAGINATION, body);

        UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(adminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));

        UserHelper.searchForUsers(staffToken, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.size", not(0));
    }

    @Test(testName = "PEG-467", dataProvider = "invalidPage", dataProviderClass = UserDataProvider.class)
    public void checkWithInvalidPage(Object page) {

        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, page);
        body.put(UserSearchBody.SIZE, 10);
        searchBody.put(UserSearchBody.PAGINATION, body);
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray
    @Test(testName = "PEG-467")
    public void checkWithInvalidPageSize() {
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, -1);
        searchBody.put(UserSearchBody.PAGINATION, body);
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test
    public void checkWithInvalidSize() {
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 2001);
        searchBody.put(UserSearchBody.PAGINATION, body);
        final Role randomRole = getRandomRole();
        UserHelper.searchForUsers(organizationAndUsers.getJSONObject(randomRole.name()).getString("token"),
                        organizationId, searchBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-472, PEG-474", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchWithNoSizeNoPage(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INCLUDE_DELETED, true);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("size", equalTo(20))
                .body("content.size()", equalTo(allUsers.size()));

        searchBody.put(UserSearchBody.LOCATION_IDS, locationIds);

        final List<JSONObject> usersWithoutWorkingLocation = new ArrayList<>();
        locationUsers.getJSONArray("no_location").forEach(user -> usersWithoutWorkingLocation.add(((JSONObject) user)));
        final List<String> usersWithWorkingLocations = allUsers.stream().distinct()
                .filter(user -> !usersWithoutWorkingLocation.contains(user))
                .map(user -> user.getString("id")).collect(Collectors.toList());

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size()", equalTo(usersWithWorkingLocations.size()));
    }

    @Test(testName = "PEG-520", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void searchForUserFromAnotherOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final String otherOrganizationId = organizationFlows.createUnpublishedOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");

        final JSONObject searchBody = new JSONObject();

        UserHelper.searchForUsers(token, otherOrganizationId, searchBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-521", dataProvider = "invalidOrganizationId", dataProviderClass = UserDataProvider.class)
    public void searchForUsersWithInvalidOrganization(Object organizationId) {
        final JSONObject searchBody = new JSONObject();

        UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-522")
    public void searchUserWithNoOrganizationId() {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.PAGINATION, new JSONObject());

        UserHelper.searchForUsers(SUPPORT_TOKEN, null, searchBody)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-1747")
    public void searchUserInDeletedOrganizationWithSupport() {
        final String organizationId = organizationFlows.createAndPublishOrganizationWithAllUsers().getJSONObject("ORGANIZATION").getString("id");
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.PAGINATION, new JSONObject());

        organizationFlows.deleteOrganization(organizationId);

        UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size()", equalTo(4));
    }

    @Test(testName = "PEG-1855", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void filterAllPOCs(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.POINT_OF_CONTACT, true);

        final List<String> pocUsersEmails = usersWithPOC.stream().map(user -> user.getString("email")).sorted().collect(Collectors.toList());

        final JSONObject pagination = new JSONObject();
        pagination.put(UserSearchBody.SORT, UserSearchBody.SortingBy.EMAIL.getAscending());
        searchBody.put(UserSearchBody.PAGINATION, pagination);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.findAll{it.email}.email", equalTo(pocUsersEmails));
    }

    @Test(testName = "PEG-1859", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void filterInactiveUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject searchBody = new JSONObject();
        final JSONArray userStatuses = new JSONArray();

        userStatuses.put("INACTIVE");

        searchBody.put(UserSearchBody.USER_STATUSES, userStatuses);

        final String inactiveUserEmail = allUsers.stream().filter(user -> user.getString("status").equals("INACTIVE"))
                .collect(Collectors.toList()).get(0).getString("email");

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.collect{it}.email[0]", equalTo(inactiveUserEmail));
    }

    @Test(testName = "PEG-1860", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void filterPOCUsersNotIncludedDeleted(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject searchBody = new JSONObject();

        searchBody.put(UserSearchBody.POINT_OF_CONTACT, true);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size()", equalTo(4));
    }

    @Test(testName = "PEG-3045, PEG-3058", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchUsersByOneLocation(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = new JSONObject();
        final JSONArray searchingLocationIds = new JSONArray();
        searchingLocationIds.put(locationIds.get(0));
        searchingLocationIds.put(UUID.randomUUID().toString());
        searchBody.put(UserSearchBody.LOCATION_IDS, searchingLocationIds);
        final JSONObject pagination = new JSONObject();
        pagination.put(UserSearchBody.SORT, UserSearchBody.SortingBy.EMAIL.getAscending());
        searchBody.put(UserSearchBody.PAGINATION, pagination);

        final List<String> location0UsersEmails = new ArrayList<>();
        locationUsers.getJSONArray(locationIds.get(0)).forEach(user -> location0UsersEmails.add(((JSONObject) user).getString("email")));
        locationUsers.getJSONArray("all_locations").forEach(user -> location0UsersEmails.add(((JSONObject) user).getString("email")));

        Collections.sort(location0UsersEmails);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(location0UsersEmails.size()))
                .body("content.collect{it.email}", equalTo(location0UsersEmails));
    }

    @Test(testName = "PEG-3057", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void userSearchIncludingLocationPartialQuery(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.LOCATION_IDS, locationIds);

        final int queryUserIndex = locationUsers.getJSONArray("all_locations").length() - 1;
        String partialQuery = locationUsers.getJSONArray("all_locations")
                .getJSONObject(queryUserIndex).getString("id").substring(0, 5);
        searchBody.put(UserSearchBody.QUERY, partialQuery);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(1))
                .body("content.collect{it.email}[0]", equalTo(locationUsers.getJSONArray("all_locations")
                        .getJSONObject(queryUserIndex).getString("email")));

        partialQuery = locationUsers.getJSONArray("all_locations")
                .getJSONObject(queryUserIndex).getString("email").substring(0, 5);
        searchBody.put(UserSearchBody.QUERY, partialQuery);
        searchBody.put(UserSearchBody.QUERY, partialQuery);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(1))
                .body("content.collect{it.id}[0]", equalTo(locationUsers.getJSONArray("all_locations")
                        .getJSONObject(queryUserIndex).getString("id")));

        partialQuery = locationUsers.getJSONArray("all_locations")
                .getJSONObject(queryUserIndex).getString("contactNumber").substring(6, 11);
        searchBody.put(UserSearchBody.QUERY, partialQuery);
        searchBody.put(UserSearchBody.QUERY, partialQuery);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(1))
                .body("content.collect{it.id}[0]", equalTo(locationUsers.getJSONArray("all_locations")
                        .getJSONObject(queryUserIndex).getString("id")));
    }

    @Test(testName = "PEG-3046", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchUsersBySeveralLocations(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.LOCATION_IDS, Arrays.asList(locationIds.get(0), locationIds.get(3)));
        final JSONObject pagination = new JSONObject();
        pagination.put(UserSearchBody.SORT, UserSearchBody.SortingBy.EMAIL.getDescending());
        searchBody.put(UserSearchBody.PAGINATION, pagination);
        final List<String> users = new ArrayList<>();
        locationUsers.getJSONArray(locationIds.get(0)).forEach(user -> users.add(((JSONObject) user).getString("email")));
        locationUsers.getJSONArray(locationIds.get(3)).forEach(user -> users.add(((JSONObject) user).getString("email")));
        locationUsers.getJSONArray("all_locations").forEach(user -> users.add(((JSONObject) user).getString("email")));

        users.sort(Collections.reverseOrder());

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(users.size()))
                .body("content.collect{it.email}", equalTo(users));
    }

    @Test(testName = "PEG-3047", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchByDifferentInvitationStatus(Role role) {

        final JSONObject organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final String organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");

        final String locationId = locationFlows.createLocation(organizationId).getString("id");

        final Role randomRole = Role.values()[getRandomInt(values().length - 2) + 1];

        final JSONObject pendingUser = userFlows.inviteUser(organizationId, randomRole, Collections.singletonList(locationId));
        final JSONObject expiredUser = userFlows.inviteUser(organizationId, randomRole, Collections.singletonList(locationId));

        DBHelper.expireInvitationToken(DBHelper.getInvitationToken(expiredUser.getString("email")));

        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.LOCATION_IDS, Collections.singletonList(locationId));
        searchBody.put(UserSearchBody.INVITATION_STATUSES, Collections.singletonList("PENDING"));

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.collect{it.email}[0]", equalTo(pendingUser.getString("email")));

        searchBody.put(UserSearchBody.INVITATION_STATUSES, Collections.singletonList("EXPIRED"));

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.collect{it.email}[0]", equalTo(expiredUser.getString("email")));
    }

    @Test(testName = "PEG-3050, PEG-3051, PEG-3107", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchWithLocationIncludingOneFromDifferentOrganization(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final String otherOrganizationLocationId = locationFlows.createLocation(otherOrganizationId).getString("id");
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.LOCATION_IDS, Arrays.asList(otherOrganizationLocationId, locationIds.get(4)));

        final JSONObject pagination = new JSONObject();
        pagination.put(UserSearchBody.SORT, UserSearchBody.SortingBy.EMAIL.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, pagination);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, true);

        final List<String> users = new ArrayList<>();
        locationUsers.getJSONArray(locationIds.get(4)).forEach(user -> users.add(((JSONObject) user).getString("email")));
        locationUsers.getJSONArray("all_locations").forEach(user -> users.add(((JSONObject) user).getString("email")));

        Collections.sort(users);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(users.size()))
                .body("content.collect{it.email}", equalTo(users));
    }


    @Test(testName = "PEG-3053", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchDeletedUsers(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INCLUDE_DELETED, false);
        final String locationIdWithDeletedUser = locationIds.get(4);
        searchBody.put(UserSearchBody.LOCATION_IDS, Collections.singletonList(locationIdWithDeletedUser));

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.collect{it.id}", not(hasItem(allUsers.get(0).getString("id"))));

        searchBody.put(UserSearchBody.INCLUDE_DELETED, true);

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.collect{it.id}", hasItem(allUsers.get(0).getString("id")));
    }

    @Test(testName = "PEG-3054")
    public void searchDeletedOrganizationUsers() {
        final JSONObject searchBody = new JSONObject();
        final JSONObject deletedOrganizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String deletedOrganizationId = deletedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        organizationFlows.deleteOrganization(deletedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id"));
        searchBody.put(UserSearchBody.LOCATION_IDS,
                Collections.singletonList(deletedOrganizationAndUsers.getJSONObject("LOCATION").getString("id")));

        final JSONObject pagination = new JSONObject();
        pagination.put(UserSearchBody.SORT, UserSearchBody.SortingBy.ID.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, pagination);

        final List<String> users = Arrays.asList(
                deletedOrganizationAndUsers.getJSONObject(OWNER.name()).getString("id"),
                deletedOrganizationAndUsers.getJSONObject(ADMIN.name()).getString("id"),
                deletedOrganizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("id"),
                deletedOrganizationAndUsers.getJSONObject(STAFF.name()).getString("id"));

        Collections.sort(users);

        UserHelper.searchForUsers(SUPPORT_TOKEN, deletedOrganizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.collect{it.id}", equalTo(users));
    }

    @Test(testName = "PEG-3055")
    public void searchUsersInUnpublishedOrganization() {
        final JSONObject unpublishedOrganization = organizationFlows.createUnpublishedOrganization();
        final String unpublishedOrganizationId = unpublishedOrganization.getString("id");
        final String unpublishedOrganizationLocationId = locationFlows.createLocation(unpublishedOrganizationId).getString("id");
        final String ownerIdWithWorkingLocation = userFlows.createUser(unpublishedOrganizationId, OWNER, Collections.singletonList(unpublishedOrganizationLocationId)).getString("id");
        final String ownerIdWithNoLocation = userFlows.createUser(unpublishedOrganizationId, OWNER, null).getString("id");
        final JSONObject searchBody = new JSONObject();

        UserHelper.searchForUsers(SUPPORT_TOKEN, unpublishedOrganizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(2))
                .body("content.collect{it.id}", hasItem(ownerIdWithNoLocation))
                .body("content.collect{it.id}", hasItem(ownerIdWithWorkingLocation));

        searchBody.put(UserSearchBody.LOCATION_IDS, Collections.singletonList(unpublishedOrganizationLocationId));

        UserHelper.searchForUsers(SUPPORT_TOKEN, unpublishedOrganizationId, searchBody)
                .then()
                .statusCode(SC_OK)
                .body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .body("content.size", equalTo(1))
                .body("content.collect{it.id}", hasItem(ownerIdWithWorkingLocation));
    }

    @Test(testName = "PEG-3105", dataProvider = "allRoles", dataProviderClass = RoleDataProvider.class)
    public void searchUsersOfPausedOrganization(Role role) {
        final JSONObject pausedOrganizationAndUsers = organizationFlows.createPausedOrganizationWithAllUsers();
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN :
                authenticationFlowHelper.getTokenWithEmail(pausedOrganizationAndUsers.getJSONObject(role.name()).getString("email"));
        final JSONObject searchBody = new JSONObject();
        final JSONObject pagination = new JSONObject();
        pagination.put(UserSearchBody.SORT, UserSearchBody.SortingBy.EMAIL.getDescending());
        searchBody.put(UserSearchBody.PAGINATION, pagination);

        final List<String> emails = Arrays.asList(
                pausedOrganizationAndUsers.getJSONObject(OWNER.name()).getString("email"),
                pausedOrganizationAndUsers.getJSONObject(ADMIN.name()).getString("email"),
                pausedOrganizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("email"),
                pausedOrganizationAndUsers.getJSONObject(STAFF.name()).getString("email")
        );

        emails.sort(Comparator.reverseOrder());

        UserHelper.searchForUsers(token, pausedOrganizationAndUsers.getJSONObject("ORGANIZATION").getString("id"), searchBody)
                .then()
                .statusCode(SC_OK)
                .body("content.size", equalTo(4))
                .body("content.collect{it.email}", equalTo(emails));
    }
}
