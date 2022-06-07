package e2e.gatewayapps.invitationresource;

import configuration.Role;
import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.userresource.data.RoleDataProvider;
import helpers.DBHelper;
import helpers.appsapi.support.invitationresource.InvitationHelper;
import helpers.flows.AuthenticationFlowHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.SupportFlows;
import org.json.*;
import org.testng.Assert;
import org.testng.annotations.*;
import java.util.*;

import static helpers.appsapi.support.invitationresource.payloads.InvitationCreationBody.*;
import static org.apache.http.HttpStatus.*;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.*;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;
import static utils.TestUtils.*;

public class InvitationSendToSupportTest extends BaseTest {
    private final String contactNumber = "+16179821732";
    private String email;
    private final String firstName = "Wanda";
    private final String lastName = "Maximoff";
    private boolean sendEmail = true;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        email = getRandomInt() + "@qless.com";
    }

    @Test(testName = "PEG-1354, PEG-1356")
    public void inviteSupportWithSupport() {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(CONTACT_NUMBER, contactNumber);
        invitePayload.put(EMAIL, email);
        invitePayload.put(FIRST_NAME, firstName);
        invitePayload.put(LAST_NAME, lastName);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/supportInvitation.json"));
    }

    @Test(testName = "PEG-1355", dataProvider = "invalid Email for Support", dataProviderClass = InvitationDataProvider.class)
    public void inviteSupportWrongEmail(Object invalidEmail) {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(CONTACT_NUMBER, contactNumber);
        invitePayload.put(EMAIL, invalidEmail);
        invitePayload.put(FIRST_NAME, firstName);
        invitePayload.put(LAST_NAME, lastName);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1357")
    public void inviteSupportWithNoEmailSent() {
        sendEmail = false;
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(CONTACT_NUMBER, contactNumber);
        invitePayload.put(EMAIL, email);
        invitePayload.put(FIRST_NAME, firstName);
        invitePayload.put(LAST_NAME, lastName);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/supportInvitation.json"))
                .assertThat().body("status", hasItem("DRAFT"));
    }

    @Test(testName = "PEG-1358")
    public void inviteSupportWithNoFirstNoLastName() {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(CONTACT_NUMBER, contactNumber);
        invitePayload.put(EMAIL, email);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/supportInvitation.json"));
    }

    @Test(testName = "PEG-1360", dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void inviteSupportWithInvalidPhone(Object invalidContactNumber) {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(CONTACT_NUMBER, invalidContactNumber);
        invitePayload.put(EMAIL, email);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1359")
    public void inviteSupportWithNoPhone() {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(EMAIL, email);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/supportInvitation.json"));
    }

    @Test(testName = "PEG-1361")
    public void inviteAlreadyInvitedSupport() {
        final JSONObject support = new SupportFlows().inviteSupport();

        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(EMAIL, support.getString("email"));

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Test(testName = "PEG-1362, PEG-1363", dataProvider = "allOrganizationRoles", dataProviderClass = RoleDataProvider.class)
    public void inviteSupportWithOwner(Role role) {
        final JSONObject organizationAndUsers = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        final String token = new AuthenticationFlowHelper().getTokenWithEmail(organizationAndUsers.getJSONObject(role.name()).getString("email"));

        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(EMAIL, email);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(token, inviteBody)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("type", is("FORBIDDEN_ACCESS"));
    }

    @Test(testName = "PEG-1375")
    public void inviteSeveralSupportsOnlyRequiredFields() {
        final JSONArray payloadBody = new JSONArray();
        final ArrayList<String> emails = new ArrayList<>();
        final int invitationsCount = 5;

        for (int i = 0; i < invitationsCount; i++) {
            String email = new Random().nextInt() + "@qless.com";
            emails.add(email);
            payloadBody.put(new JSONObject().put(EMAIL, email));
        }

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        final ArrayList<String> emailList = InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .path("collect{it.email}");
        Collections.sort(emails);
        Collections.sort(emailList);
        assertEquals(emails, emailList);
    }

    @Test(testName = "PEG-1374")
    public void inviteSeveralSupports() {
        final JSONArray payloadBody = new JSONArray();
        final ArrayList<String> emails = new ArrayList<>();
        final int invitationsCount = 3;

        for (int i = 0; i < invitationsCount; i++) {
            String email = new Random().nextInt() + "@qless.com";
            emails.add(email);
            payloadBody.put(new JSONObject().put(EMAIL, email)
                    .put(FIRST_NAME, firstName)
                    .put(LAST_NAME, lastName)
                    .put(CONTACT_NUMBER, contactNumber));
        }

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        final ArrayList<String> emailsResponse = InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .path("collect{it.email}");
        Collections.sort(emails);
        Collections.sort(emailsResponse);
        assertEquals(emails, emailsResponse);
    }

    @Test(testName = "PEG-1376")
    public void inviteSeveralSupportsSamePayloads() {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(EMAIL, email);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);
        payloadBody.put(invitePayload);
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        final ArrayList<String> emailList = InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .path("collect{it.email}");

        Assert.assertEquals(emailList.size(), 1);
    }

    @Test(testName = "PEG-1378")
    public void inviteSeveralSupportsSameEmailDifferentNames() {
        final JSONArray payloadBody = new JSONArray();
        final int invitationsCount = 3;

        for (int i = 0; i < invitationsCount; i++) {
            payloadBody.put(new JSONObject().put(EMAIL, email)
                    .put(FIRST_NAME, firstName + i)
                    .put(LAST_NAME, lastName + i)
                    .put(CONTACT_NUMBER, contactNumber));
        }

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
    }

    @Test(testName = "PEG-1377")
    public void inviteSeveralSupportsAlreadySent() {
        final JSONObject invitePayload = new JSONObject();
        invitePayload.put(EMAIL, email);

        final JSONArray payloadBody = new JSONArray();
        payloadBody.put(invitePayload);

        final JSONObject inviteBody = new JSONObject();
        inviteBody.put(PAYLOADS, payloadBody);
        inviteBody.put(SEND_EMAIL, sendEmail);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CREATED);

        final String emailNew = new Random().nextInt() + "@qless.com";
        final JSONObject invitePayload1 = new JSONObject();
        invitePayload1.put(EMAIL, emailNew);

        payloadBody.put(invitePayload1);

        InvitationHelper.inviteSupports(SUPPORT_TOKEN, inviteBody)
                .then()
                .statusCode(SC_CONFLICT)
                .body("type", is("RESOURCE_ALREADY_EXISTS"));
        Assert.assertNull(DBHelper.getAccountFieldValueByEmail(emailNew, "email"));
    }
}
