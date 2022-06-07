package e2e.gatewayapps.userresource;


import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.userresource.data.UserDetailsDataProvider;
import helpers.flows.LocationFlows;
import helpers.flows.OrganizationFlows;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.UserFlows;
import org.apache.http.HttpStatus;
import org.json.*;
import org.testng.Assert;
import org.testng.annotations.*;
import helpers.appsapi.usersresource.payloads.UserSearchBody;
import utils.Xray;

import java.util.*;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static configuration.Role.*;
import static org.hamcrest.Matchers.is;
import static pages.UsersListPage.STATUS.EXPIRED;
import static pages.UsersListPage.STATUS.INACTIVE;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.getRandomInt;

public class UserFilterTest extends BaseTest {

    private final String getUserStatusPath = "content.collect{it.userStatus}";

    private OrganizationFlows organizationFlows;
    private String organizationId;
    private JSONObject organizationAndUsers;

    private String ownerToken;
    private String adminToken;
    private String locationAdminToken;
    private String staffToken;

    @BeforeClass
    public void dataPreparation() {
        organizationFlows = new OrganizationFlows();
        final UserFlows userFlows = new UserFlows();
        organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();

        organizationId = organizationAndUsers.getJSONObject("ORGANIZATION").getString("id");
        final String locationId = organizationAndUsers.getJSONObject("LOCATION").getString("id");
        final String locationId1 = new LocationFlows().createLocation(organizationId).getString("id");
        final List<String> locations = Arrays.asList(locationId1, locationId);

        ownerToken = organizationAndUsers.getJSONObject(OWNER.name()).getString("token");
        adminToken = organizationAndUsers.getJSONObject(ADMIN.name()).getString("token");
        locationAdminToken = organizationAndUsers.getJSONObject(LOCATION_ADMIN.name()).getString("token");
        staffToken = organizationAndUsers.getJSONObject(STAFF.name()).getString("token");

        for (int i = 0; i < 30; i++) {
            final Role randomRole = Arrays.asList(Role.values()).get(getRandomInt(Role.values().length - 2) + 1);
            final JSONObject userCreated = userFlows.createUser(organizationId, randomRole, locations);
            final JSONObject userInvited = userFlows.createUser(organizationId, randomRole, locations);
            if (i % 2 == 0) {
                userFlows.inactivateUserById(organizationId, userCreated.getString("id"));
                userFlows.inactivateUserById(organizationId, userInvited.getString("id"));
            }
        }
    }

    @Test(testName = "PEG-956", dataProvider = "validAccountStatus", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByStatus(Object status) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.USER_STATUSES, Collections.singletonList(status));

        ArrayList<String> accountStatuses = UserHelper.searchForUsers(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path(getUserStatusPath);
        accountStatuses.forEach(invite -> Assert.assertEquals(invite, status));

        accountStatuses = UserHelper.searchForUsers(staffToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path(getUserStatusPath);
        accountStatuses.forEach(invite -> Assert.assertEquals(invite, status));

        accountStatuses = UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path(getUserStatusPath);
        accountStatuses.forEach(invite -> Assert.assertEquals(invite, status));
    }

    @Test(testName = "PEG-959")
    public void filterByStatuses() {
        final String[] status = {"ACTIVE", "INACTIVE"};
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.USER_STATUSES, status);

        ArrayList<String> accountStatuses = UserHelper.searchForUsers(adminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path(getUserStatusPath);

        accountStatuses.forEach((account) -> {
            boolean match = (account.equals(status[0]) || account.equals(status[1]));
            Assert.assertTrue(match);
        });

        accountStatuses = UserHelper.searchForUsers(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path(getUserStatusPath);
        accountStatuses.forEach((account) -> {
            boolean match = (account.equals(status[0]) || account.equals(status[1]));
            Assert.assertTrue(match);
        });

    }

    @Test(testName = "PEG-957", dataProvider = "invalidAccountStatus", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByInvalidStatus(Object status) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.USER_STATUSES, new JSONArray().put(status));
        final Role randomOrganizationRole = getRandomOrganizationRole();
        final String token = randomOrganizationRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(randomOrganizationRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Test(testName = "PEG-986", dataProvider = "invalid Account Statuses", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByInvalidAccountStatuses(Object firstStatus, Object secondStatus) {
        final JSONObject searchBody = new JSONObject();
        final JSONArray statuses = new JSONArray();
        statuses.put(firstStatus);
        statuses.put(secondStatus);
        searchBody.put(UserSearchBody.USER_STATUSES, statuses);
        final Role randomOrganizationRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomOrganizationRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Xray(test = "")
    @Test
    public void filterByInvalidAccountStatusesValue() {
        final JSONObject searchBody = new JSONObject();
        final JSONArray statuses = new JSONArray();
        statuses.put(INACTIVE);
        statuses.put(JSONObject.NULL);
        searchBody.put(UserSearchBody.USER_STATUSES, statuses);
        final Role randomOrganizationRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomOrganizationRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-984")
    public void filterByStatusWithOtherOrganizationId() {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.USER_STATUSES, new JSONArray().put("ACTIVE"));
        final String organizationId = new OrganizationFlows().createAndPublishOrganizationWithOwner().getJSONObject("ORGANIZATION").getString("id");
        final Role randomOrganizationRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomOrganizationRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-960", dataProvider = "validInvitationStatus", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByInvitationStatus(Object status) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INVITATION_STATUSES, new JSONArray().put(status));

        ArrayList<String> invitationStatuses = UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach(invite -> Assert.assertEquals(invite, status));

        invitationStatuses = UserHelper.searchForUsers(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach(invite -> Assert.assertEquals(invite, status));

        invitationStatuses = UserHelper.searchForUsers(adminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach(invite -> Assert.assertEquals(invite, status));

        invitationStatuses = UserHelper.searchForUsers(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach(invite -> Assert.assertEquals(invite, status));

        invitationStatuses = UserHelper.searchForUsers(staffToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach(invite -> Assert.assertEquals(invite, status));
    }

    @Test(testName = "PEG-961", dataProvider = "invitation2StatusMix", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByInvitationStatuses(Object firstStatus, Object secondStatus) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INVITATION_STATUSES, Arrays.asList(firstStatus, secondStatus));

        ArrayList<String> invitationStatuses = UserHelper.searchForUsers(staffToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
                    boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus));
                    Assert.assertTrue(match);
                }
        );
        invitationStatuses = UserHelper.searchForUsers(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
                    boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus));
                    Assert.assertTrue(match);
                }
        );
        invitationStatuses = UserHelper.searchForUsers(adminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
                    boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus));
                    Assert.assertTrue(match);
                }
        );
        invitationStatuses = UserHelper.searchForUsers(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
                    boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus));
                    Assert.assertTrue(match);
                }
        );
        invitationStatuses = UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
                    boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus));
                    Assert.assertTrue(match);
                }
        );
    }

    @Test(testName = "PEG-962", dataProvider = "invalidInvitation2StatusMix", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByInvalidStatuses(Object firstStatus, Object secondStatus) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.USER_STATUSES, Arrays.asList(firstStatus, secondStatus));
        final Role randomOrganizationRole = getRandomOrganizationRole();
        final String token = organizationAndUsers.getJSONObject(randomOrganizationRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Test(testName = "PEG-7310")
    public void filterByInvalidStatusesValues() {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.USER_STATUSES, Arrays.asList(JSONObject.NULL));
        final Role randomOrganizationRole = getRandomOrganizationRole();
        final String token = randomOrganizationRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(randomOrganizationRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-961", dataProvider = "invitation3StatusMix", dataProviderClass = UserDetailsDataProvider.class)
    void filterByInvitation3Statuses(Object firstStatus, Object secondStatus, Object thirdStatus) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INVITATION_STATUSES, Arrays.asList(firstStatus, secondStatus, thirdStatus));

        ArrayList<String> invitationStatuses = UserHelper.searchForUsers(ownerToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
            boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus) || invite.equals(thirdStatus));
            Assert.assertTrue(match);
        });

        invitationStatuses = UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
            boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus) || invite.equals(thirdStatus));
            Assert.assertTrue(match);
        });

        invitationStatuses = UserHelper.searchForUsers(staffToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
            boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus) || invite.equals(thirdStatus));
            Assert.assertTrue(match);
        });

        invitationStatuses = UserHelper.searchForUsers(adminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
            boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus) || invite.equals(thirdStatus));
            Assert.assertTrue(match);
        });

        invitationStatuses = UserHelper.searchForUsers(locationAdminToken, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path("content.collect{it.invitationStatus}");
        invitationStatuses.forEach((invite) -> {
            boolean match = (invite.equals(firstStatus) || invite.equals(secondStatus) || invite.equals(thirdStatus));
            Assert.assertTrue(match);
        });
    }


    @Test(testName = "PEG-965", dataProvider = "invalidInvitationStatus", dataProviderClass = UserDetailsDataProvider.class)
    public void filterByInvalidInvitationStatus(Object status) {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INVITATION_STATUSES, Collections.singletonList(status));

        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN :
                organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Test(testName = "PEG-7312")
    public void filterByInvalidInvitationStatusValue() {
        final JSONObject searchBody = new JSONObject();
        searchBody.put(UserSearchBody.INVITATION_STATUSES, Collections.singletonList(JSONObject.NULL));
        final Role randomRole = getRandomRole();
        final String token = randomRole.equals(SUPPORT) ? SUPPORT_TOKEN : organizationAndUsers.getJSONObject(randomRole.name()).getString("token");
        UserHelper.searchForUsers(token, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1746")
    public void filterByStatusOfDeletedOrganizationWithSupport() {
        final JSONObject unpublishedOrganizationWithOwner = organizationFlows.createAndPublishOrganizationWithAllUsers();
        final String organizationId = unpublishedOrganizationWithOwner.getJSONObject("ORGANIZATION").getString("id");
        final JSONObject searchBody = new JSONObject();
        organizationFlows.deleteOrganization(organizationId);
        searchBody.put(UserSearchBody.USER_STATUSES, new JSONArray().put("ACTIVE"));

        final ArrayList<String> accountStatuses = UserHelper.searchForUsers(SUPPORT_TOKEN, organizationId, searchBody)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/usersList.json"))
                .extract()
                .path(getUserStatusPath);

        int expectedUserCount = 4;
        accountStatuses.forEach(invite -> Assert.assertEquals(invite, "ACTIVE"));
        Assert.assertEquals(accountStatuses.size(), expectedUserCount);
    }
}
