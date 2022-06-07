package helpers.flows;

import helpers.DBHelper;
import helpers.appsapi.invitationresource.payloads.InvitationAcceptBody;
import helpers.appsapi.support.invitationresource.InvitationHelper;
import helpers.appsapi.support.invitationresource.payloads.InvitationCreationBody;
import helpers.appsapi.support.usersresource.UsersHelper;
import io.restassured.response.ValidatableResponse;
import org.json.JSONArray;
import org.json.JSONObject;


import java.util.List;

import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class SupportFlows {

    public JSONArray inviteSupportsByEmails(List<String> emails) {
        final int invitationCount = emails.size();
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreationBody.SupportInvitationCreationCombination.REQUIRED, invitationCount);
        final JSONArray payloads = invitationBody.getJSONArray(InvitationCreationBody.PAYLOADS);
        for (int i = 0; i < invitationCount; i++) {
            payloads.getJSONObject(i).put(InvitationCreationBody.EMAIL, emails.get(i));
        }
        final ValidatableResponse response = InvitationHelper.inviteSupports(SUPPORT_TOKEN, invitationBody).then().statusCode(SC_CREATED);
        return new JSONArray(response.extract().body().asString());
    }

    public JSONArray inviteSupports(int count) {
        final JSONObject invitationBody = InvitationCreationBody.bodyBuilder(InvitationCreationBody.SupportInvitationCreationCombination.ALL_FIELDS, count);
        final ValidatableResponse response = helpers.appsapi.support.invitationresource.InvitationHelper.inviteSupports(SUPPORT_TOKEN, invitationBody)
                .then().statusCode(SC_CREATED);

        return new JSONArray(response.extract().body().asString());
    }

    public JSONObject inviteSupport() {
        return inviteSupports(1).getJSONObject(0);
    }

    public JSONArray createSupports(List<String> emails) {
        inviteSupportsByEmails(emails);
        final JSONArray invitationAcceptBodies = new JSONArray();
        for (String email : emails) {
            final String invitationToken = DBHelper.getInvitationToken(email);
            final JSONObject acceptBody = new InvitationAcceptBody().bodyBuilder();
            acceptBody.put(InvitationAcceptBody.TOKEN, invitationToken);
            final ValidatableResponse response = helpers.appsapi.invitationresource.InvitationHelper.acceptInvite(acceptBody).then().statusCode(SC_CREATED);
            final JSONObject responseBody = new JSONObject(response.extract().body().asString());
            responseBody.getJSONObject("user").put("token", responseBody.getString("token"));
            invitationAcceptBodies.put(responseBody.getJSONObject("user"));
        }
        return invitationAcceptBodies;
    }

    public JSONObject createSupport(String email) {
        return createSupports(List.of(email)).getJSONObject(0);
    }

    public void deleteSupport(String supportId){
        UsersHelper.deleteSupportUser(SUPPORT_TOKEN, supportId);
    }



}
