package e2e.gatewayapps.invitationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.invitationresource.InvitationHelper;
import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;
import helpers.appsapi.usersresource.UserHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.testng.log4testng.Logger;

import java.util.Collections;

import static configuration.Role.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.is;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class InvitationAcceptTest extends BaseTest {

    private final Logger log = Logger.getLogger(InvitationAcceptTest.class);
    private String orgId;
    private String locationId;
    private final UserFlows userFlows = new UserFlows();

    @BeforeClass
    public void setup() {
        final OrganizationFlows organizationFlows = new OrganizationFlows();
        final JSONObject orgAndOwner = organizationFlows.createAndPublishOrganizationWithAllUsers();
        orgId = orgAndOwner.getJSONObject("ORGANIZATION").getString("id");
        locationId = orgAndOwner.getJSONObject("LOCATION").getString("id");
    }

    @Test(testName = "PEG-318 & PEG-311", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    void acceptInvitation(Role role) {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final UserFlows userFlows = new UserFlows();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, role, Collections.singletonList(locationId));
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));

        String accountToken = InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("account.email", is(invitationResponse.getString("email")));
    }

    @Test(testName = "PEG-320")
    void acceptInviteWithExpiredToken() {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final UserFlows userFlows = new UserFlows();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        final String invitationToken = invitationResponse.getString("token");
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
        DBHelper.expireInvitationToken(invitationToken);

        if (invitationToken != null) {
            acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
            InvitationHelper.acceptInvite(acceptBody)
                    .then()
                    .statusCode(HttpStatus.SC_GONE)
                    .body("type", is("RESOURCE_NOT_VIABLE"));
        } else {
            log.error("expired token is not found");
        }
    }

    @Test(testName = "PEG-322")
    void acceptWithoutToken() {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(InvitationAcceptBody.TOKEN, JSONObject.NULL);
        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-321")
    void acceptAlreadyAcceptedInvite() {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat().body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"));
        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_GONE)
                .body("type", is("RESOURCE_NOT_VIABLE"));
    }

    @Test(testName = "PEG-350 & PEG-351 & PEG-353")
    void acceptInvitationWithoutRequiredPayload() {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));
        acceptBody.remove(InvitationAcceptBody.FIRST_NAME);
        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

        acceptBody.put(InvitationAcceptBody.FIRST_NAME, "QA");
        acceptBody.put(InvitationAcceptBody.LAST_NAME, JSONObject.NULL);
        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));

        acceptBody.remove(InvitationAcceptBody.PASSWORD);
        acceptBody.put(InvitationAcceptBody.LAST_NAME, "QA");

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-354", dataProvider = "invalidPassword", dataProviderClass = InvitationDataProvider.class)
    void acceptInvitationWithInvalidPassword(Object password) {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));
        acceptBody.put(InvitationAcceptBody.PASSWORD, password);

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-354")
    void acceptInvitationWithInvalidTypeOfPassword() {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));
        acceptBody.put(InvitationAcceptBody.PASSWORD, true);

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Test(testName = "PEG-356", dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    void acceptInvitationWithInvalidPhoneNumber(Object phoneNumber) {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));
        acceptBody.put(InvitationAcceptBody.CONTACT_NUMBER, phoneNumber);

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-356")
    void acceptInvitationWithInvalidTypeOfPhoneNumber() {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));
        acceptBody.put(InvitationAcceptBody.CONTACT_NUMBER, true);

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("type", is("NOT_READABLE_REQUEST_BODY"));
    }

    @Test(testName = "PEG-355", dataProvider = "validPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    void acceptInvitationWithValidPhoneNumber(Object phoneNumber) {
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        final JSONObject invitationResponse = userFlows.inviteUser(orgId, OWNER, null);
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationResponse.getString("token"));
        acceptBody.put(InvitationAcceptBody.CONTACT_NUMBER, phoneNumber);

        final JSONObject userProperties = new JSONObject(InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/acceptInvitation.json"))
                .extract().body().asString());
        final String accountToken = userProperties.getString("token");
        final String accountEmail = userProperties.getJSONObject("user").getString("email");
        final String userId = DBHelper.getUserIdByEmail(accountEmail);
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(HttpStatus.SC_OK);

        UserHelper.getUserById(SUPPORT_TOKEN, orgId, userId)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .assertThat()
                .body("contactNumber", is(phoneNumber));
    }

}
