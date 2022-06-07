package e2e.gatewayapps.invitationresource;

import e2e.gatewayapps.BaseTest;
import e2e.gatewayapps.invitationresource.data.InvitationDataProvider;
import helpers.DBHelper;
import helpers.appsapi.accountresource.AccountHelper;
import helpers.appsapi.support.invitationresource.flows.SupportFlows;
import helpers.appsapi.invitationresource.InvitationHelper;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.Random;

import static configuration.Role.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static utils.TestUtils.*;

import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;


public class InvitationAcceptBySupportTest extends BaseTest {

    private final String emailDomainName = "@qless.com";

    @Test(testName = "PEG-1379")
    public void acceptInvitation() {
        final String token = getToken(SUPPORT);
        final String email = new Random().nextInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);
        final String invitationToken = DBHelper.getInvitationToken(email);

        final JSONObject invitationAcceptBody = new InvitationAcceptBody().bodyBuilder();
        invitationAcceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        final String accountToken = InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .path("token");
        AccountHelper.getCurrentAccount(accountToken)
                .then()
                .statusCode(SC_OK)
                .assertThat()
                .body("account.email", is(email));
    }

    @Test(testName = "PEG-1381")
    public void acceptInvitationWithExpiredToken() {
        final String token = getToken(SUPPORT);
        final String email = getRandomInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);
        final String invitationToken = DBHelper.getInvitationToken(email);
        DBHelper.expireInvitationToken(invitationToken);

        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        InvitationHelper.acceptInvite(acceptBody)
                .then()
                .statusCode(SC_GONE)
                .body("type", is("RESOURCE_NOT_VIABLE"));
    }

    @Test(testName = "PEG-1381")
    public void acceptInvitationWithoutToken() {
        final String token = getToken(SUPPORT);
        final String email = getRandomInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);

        final JSONObject invitationAcceptBody = new InvitationAcceptBody().bodyBuilder();

        InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1382")
    public void acceptInvitationAlreadyAccepted() {
        final String token = getToken(SUPPORT);
        final String email = new Random().nextInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);
        final String invitationToken = DBHelper.getInvitationToken(email);

        final JSONObject invitationAcceptBody = new InvitationAcceptBody().bodyBuilder();
        invitationAcceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_CREATED);

        InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_GONE)
                .body("type", is("RESOURCE_NOT_VIABLE"));
    }

    @Test(testName = "PEG-1383", dataProvider = "invalidPassword", dataProviderClass = InvitationDataProvider.class)
    public void acceptInvitationInvalidPassword(Object invalidPassword) {
        final String token = getToken(SUPPORT);
        final String email = new Random().nextInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);
        final String invitationToken = DBHelper.getInvitationToken(email);

        final JSONObject invitationAcceptBody = new InvitationAcceptBody().bodyBuilder();
        invitationAcceptBody.put(InvitationAcceptBody.PASSWORD, invalidPassword);
        invitationAcceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1384", dataProvider = "invalidPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void acceptInvitationInvalidPhone(Object invalidPhone) {
        final String token = getToken(SUPPORT);
        final String email = new Random().nextInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);
        final String invitationToken = DBHelper.getInvitationToken(email);

        final JSONObject invitationAcceptBody = new InvitationAcceptBody().bodyBuilder();
        invitationAcceptBody.put(InvitationAcceptBody.CONTACT_NUMBER, invalidPhone);
        invitationAcceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("type", is("DATA_INTEGRITY_CONSTRAINT_VIOLATED"));
    }

    @Test(testName = "PEG-1385", dataProvider = "validPhoneNumber", dataProviderClass = InvitationDataProvider.class)
    public void acceptInvitationValidPhone(Object validPhoneNumber) {
        final String token = getToken(SUPPORT);
        final String email = new Random().nextInt() + emailDomainName;
        final SupportFlows flows = new SupportFlows();
        flows.inviteSupport(token, email);
        final String invitationToken = DBHelper.getInvitationToken(email);

        final JSONObject invitationAcceptBody = new InvitationAcceptBody().bodyBuilder();
        invitationAcceptBody.put(InvitationAcceptBody.CONTACT_NUMBER, validPhoneNumber);
        invitationAcceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

        InvitationHelper.acceptInvite(invitationAcceptBody)
                .then()
                .statusCode(SC_CREATED);
    }
}
