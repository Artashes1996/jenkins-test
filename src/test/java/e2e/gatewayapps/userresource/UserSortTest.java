package e2e.gatewayapps.userresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.appsapi.locationsresource.payloads.LocationsSearchRequestBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.appsapi.usersresource.payloads.UserSearchBody;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;

import java.util.*;

import static configuration.Role.*;
import static helpers.appsapi.locationsresource.payloads.LocationsSearchRequestBody.SortingBy.*;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class UserSortTest extends BaseTest {

    private OrganizationFlows organizationFlows;
    private JSONObject organizationAndUsers;
    private String organizationId;

    @BeforeClass
    public void setUp() {
        organizationFlows = new OrganizationFlows();
        final UserFlows userFlows = new UserFlows();
        organizationAndUsers = organizationFlows.createAndPublishOrganizationWithAllUsers();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");

        for (int i = 0; i < 20; i++) {
            final Role randomRole = Arrays.asList(Role.values()).get(getRandomInt(Role.values().length - 2) + 1);
            userFlows.createUser(organizationId, randomRole, Collections.singletonList(locationId));
        }
    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkAscNameOrderForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();

        final JSONObject body = new JSONObject();
        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 10);
        body.put(UserSearchBody.SORT, FIRST_NAME.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "false");

        final ArrayList<String> names = UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content.findAll{it.firstName}.firstName");

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        assertEquals(copy, names);
    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkDescNameOrderForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 10);
        body.put(UserSearchBody.SORT, FIRST_NAME.getDescending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "false");

        final ArrayList<String> names = UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content.findAll{it.firstName}.firstName");

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        Collections.reverse(names);
        assertEquals(copy, names);

    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkAscLastNameOrderForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();

        final JSONObject body = new JSONObject();
        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 10);
        body.put(UserSearchBody.SORT, LAST_NAME.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "false");

        final ArrayList<String> names = UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("content.findAll{it.lastName}.lastName");

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        assertEquals(copy, names);
    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkSortDescByEmailForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();

        final JSONObject body = new JSONObject();
        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 20);
        body.put(UserSearchBody.SORT, EMAIL.getDescending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "true");

        final ArrayList<String> emails = UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .extract()
                .path("content*.email");

        final ArrayList<String> copy = new ArrayList<>(emails);
        Collections.sort(emails);
        Collections.reverse(emails);
        assertEquals(copy, emails);

    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkSortAscByEmailForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN: organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();

        final JSONObject body = new JSONObject();
        body.put(UserSearchBody.PAGE, 1);
        body.put(UserSearchBody.SIZE, 20);
        body.put(UserSearchBody.SORT, EMAIL.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "true");

        final ArrayList<String> emails = UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .extract()
                .path("content*.email");

        final ArrayList<String> copy = new ArrayList<>(emails);
        Collections.sort(emails);
        assertEquals(copy, emails);

    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkSortAscByUserStatusForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 100);
        body.put(UserSearchBody.SORT, LocationsSearchRequestBody.SortingBy.USER_STATUS.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "true");

        final Response response = UserHelper.searchForUsers(token, organizationId, searchBody);
        final ArrayList<String> names = response
                .then()
                .extract()
                .path("content*.userStatus");

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        assertEquals(copy, names);

        final ArrayList<String> activeAccounts = response
                .then()
                .extract()
                .path("content.findAll {it.accountStatus == 'ACTIVE'}.email");

        final ArrayList<String> activeAccountCopy = new ArrayList<>(activeAccounts);

        Collections.sort(activeAccounts);
        assertEquals(activeAccountCopy, activeAccounts);

    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkSortDescByUserStatusForOwner(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.PAGE, 0);
        body.put(UserSearchBody.SIZE, 100);
        body.put(UserSearchBody.SORT, USER_STATUS.getAscending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "true");

        final Response response = UserHelper.searchForUsers(token, organizationId, searchBody);
        final ArrayList<String> names = response
                .then()
                .extract()
                .path("content*.userStatus");

        final ArrayList<String> copy = new ArrayList<>(names);
        Collections.sort(names);
        assertEquals(copy, names);

        final ArrayList<String> inactiveAccounts = response
                .then()
                .extract()
                .path("content.findAll {it.userStatus == 'INACTIVE'}.email");

        final ArrayList<String> inactiveAccountsCopy = new ArrayList<>(inactiveAccounts);

        Collections.sort(inactiveAccounts);
        assertEquals(inactiveAccountsCopy, inactiveAccounts);

    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allRoles")
    public void checkSortWithUserStatus(Role role) {
        final String token = role.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(role.name()).getString("token");
        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.SIZE, 100);
        body.put(UserSearchBody.SORT, LocationsSearchRequestBody.SortingBy.USER_STATUS.getDescending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "true");

        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK);

    }

    @Test(dataProviderClass = RoleDataProvider.class, dataProvider = "allOrganizationRoles")
    public void checkSortWithOtherOrganizationUser(Role role) {
        final String token = organizationAndUsers.getJSONObject(role.name()).getString("token");
        final String otherOrganizationId = organizationFlows.createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");

        final JSONObject searchBody = new JSONObject();
        final JSONObject body = new JSONObject();

        body.put(UserSearchBody.SIZE, 100);
        body.put(UserSearchBody.SORT, LocationsSearchRequestBody.SortingBy.USER_STATUS.getDescending());

        searchBody.put(UserSearchBody.PAGINATION, body);
        searchBody.put(UserSearchBody.INCLUDE_DELETED, "true");

        UserHelper.searchForUsers(token, otherOrganizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

}
