package helpers.appsapi.invitationresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class InvitationHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("/invitations");
    final static RequestSpecification orgIdRequestSpecification = setUpSpec("/organizations/");

    public static Response inviteUsersLegacy(Object token, Object body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response inviteUsers(Object token, Object orgId, Object body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(orgId + "/invitations");
    }

    public static Response getInvitationByToken(Object emailToken) {
        return given()
                .spec(requestSpecification)
                .get("/token/" + emailToken);
    }

    public static Response acceptInvite(JSONObject acceptBody) {
        return given()
                .spec(requestSpecification)
                .body(acceptBody.toString())
                .post("/accept");
    }


    public static Response resendInvitation(Object token, Object organizationId, JSONObject body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/invitations/resend");
    }

}



