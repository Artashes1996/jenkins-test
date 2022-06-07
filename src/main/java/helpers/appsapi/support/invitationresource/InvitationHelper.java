package helpers.appsapi.support.invitationresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class InvitationHelper extends BaseAPIHelper {

    final static RequestSpecification supportRequestSpecification = setUpSpec("/support/invitations");

    public static Response inviteSupports(Object token, JSONObject body) {

        return given()
                .spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }
}
