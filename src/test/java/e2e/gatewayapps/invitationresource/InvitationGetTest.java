package e2e.gatewayapps.invitationresource;

import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import e2e.gatewayapps.BaseTest;
import helpers.DBHelper;
import helpers.flows.OrganizationFlows;
import helpers.flows.UserFlows;
import helpers.appsapi.invitationresource.InvitationHelper;
import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.*;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;

public class InvitationGetTest extends BaseTest {

    private String organizationId;
    private final UserFlows userFlows = new UserFlows();


    @BeforeClass
    public void setUp() {
        final JSONObject organizationAndUser = new OrganizationFlows().createAndPublishOrganizationWithAllUsers();
        organizationId = organizationAndUser.getJSONObject("ORGANIZATION").getString("id");
    }

    @Test(testName = "PEG-314")
    public void getInvitationByToken() {
        final String email = userFlows.inviteUser(organizationId, OWNER, null).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);

        InvitationHelper.getInvitationByToken(invitationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test(testName = "PEG-314", dataProvider = "invalidToken", dataProviderClass = InvitationDataProvider.class)
    public void getInvitationByInvalidToken(Object invalidToken) {
        InvitationHelper.getInvitationByToken(invalidToken)
                .then()
                .statusCode(SC_NOT_FOUND)
                .body("type", is("RESOURCE_NOT_FOUND"));
    }

    @Test(testName = "PEG-316")
    public void getInvitationByExpiredToken() {
        final String email = userFlows.inviteUser(organizationId, OWNER, null).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);
        DBHelper.expireInvitationToken(invitationToken);

        InvitationHelper.getInvitationByToken(invitationToken)
                .then()
                .statusCode(HttpStatus.SC_GONE)
                .body("type", is("RESOURCE_NOT_VIABLE"));
    }

    @Test(testName = "PEG-317")
    public void getInvitationByAcceptedToken() {
        final JSONObject acceptBody = new JSONObject();

        final String email = userFlows.inviteUser(organizationId, OWNER, null).getString("email");
        final String invitationToken = DBHelper.getInvitationToken(email);

        if (invitationToken != null) {
            acceptBody.put(InvitationAcceptBody.PASSWORD, "Qw123456!");
            acceptBody.put(InvitationAcceptBody.LAST_NAME, "Qa");
            acceptBody.put(InvitationAcceptBody.FIRST_NAME, "QA");
            acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

            InvitationHelper.acceptInvite(acceptBody)
                    .then()
                    .statusCode(HttpStatus.SC_CREATED);

            InvitationHelper.getInvitationByToken(invitationToken)
                    .then()
                    .statusCode(SC_OK)
                    .body("status", is("ACCEPTED"));
        }

    }
}
