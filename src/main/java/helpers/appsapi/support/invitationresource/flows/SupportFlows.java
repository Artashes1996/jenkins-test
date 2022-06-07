package helpers.appsapi.support.invitationresource.flows;

import helpers.DBHelper;
import helpers.appsapi.support.invitationresource.InvitationHelper;
import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;
import org.json.JSONArray;
import org.json.JSONObject;

import static helpers.appsapi.invitationresource.payloads.InvitationAcceptBody.PASSWORD;
import static helpers.appsapi.support.invitationresource.payloads.InvitationCreationBody.*;
import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;

public class SupportFlows {

    public String inviteSupport(Object token, String contactNumber, String email, String firstName, String lastName, Boolean sendEmail) {
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

        InvitationHelper.inviteSupports(token, inviteBody)
                .then()
                .statusCode(SC_CREATED);
        return DBHelper.getInvitationToken(email);
    }

    public String inviteSupport(Object token, String email, Boolean sendEmail) {
        return inviteSupport(token, null, email, null, null, sendEmail);
    }

    public String inviteSupport(Object token, String email) {
        return inviteSupport(token, email, true);
    }

    public String createSupport(Object token, String email, String firstName, String lastName, String password){
        final String invitationToken = inviteSupport(token, email);
        final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
        acceptBody.put(FIRST_NAME, firstName);
        acceptBody.put(LAST_NAME, lastName);
        acceptBody.put(PASSWORD, password);
        if (invitationToken != null) {
            acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);

            return helpers.appsapi.invitationresource.InvitationHelper.acceptInvite(acceptBody)
                    .then()
                    .statusCode(SC_CREATED)
                    .extract()
                    .path("user.id");
        }
        return null;
    }

    public String createSupport(Object token, String email, String password){
        final String firstName = "Vision";
        final String lastName = "Unknown/Maximoff";
        return createSupport(token, email, firstName, lastName, password);
    }

    public String createSupport(Object token, String email){
        final String password = "Qw!123456";
        return createSupport(token, email, password);
    }

}