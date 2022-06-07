package helpers.appsapi.accountresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class AccountHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("/accounts/");
    final static RequestSpecification orgIdRequestSpecification = setUpSpec("/organizations/");

    public static Response getCurrentAccount(Object token) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get("current");
    }

    public static Response changePassword(Object token, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .body(body.toString())
                .header("Authorization", "Bearer " + token)
                .put("current/change-password");
    }

    public static Response getAccountById(Object token, Object organizationId, Object accountId) {

        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/accounts/" + accountId);
    }

    public static Response getDeletedAccountById(String token, String organizationId, String accountId) {

        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .queryParam("includeDeleted", true)
                .get(organizationId + "/accounts/" + accountId);
    }

    public static Response restoreApply(JSONObject body) {
        return given()
                .spec(requestSpecification)
                .body(body.toString())
                .post("restore/apply");
    }

    public static Response resetPasswordRequest(JSONObject body) {
        return given()
                .spec(requestSpecification)
                .body(body.toString())
                .post("password/reset/request");
    }

    public static Response applyResetPassword(JSONObject body) {
        return given()
                .spec(requestSpecification)
                .body(body.toString())
                .post("password/reset/apply");
    }

    public static Response forceResetPasswordRequest(Object token, Object organizationId, JSONObject body) {
        return given().spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/accounts/password/reset/force");
    }

    public static Response getDeletedAccountByEmail(Object token, String email) {
        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(email + "/deleted");
    }

    public static Response getRestoreAccountByToken(Object token) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get("restore/token/" + token);
    }

    public static Response getResetPasswordByToken(Object token) {
        return given()
                .spec(requestSpecification)
                .get("password/reset/token/" + token);
    }

}
