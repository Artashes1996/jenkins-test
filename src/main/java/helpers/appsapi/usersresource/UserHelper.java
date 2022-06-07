package helpers.appsapi.usersresource;

import helpers.BaseAPIHelper;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import utils.commons.ToggleAction;

import java.io.File;

import static io.restassured.RestAssured.given;

public class UserHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("/users");
    final static RequestSpecification orgIdRequestSpecification = setUpSpec("/organizations/");
    final static RequestSpecification formDataRequestSpecification = setUpSpec("/users").contentType("multipart/form-data");

    public static Response searchForUsers(Object token, Object organizationId, JSONObject body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/users/search");
    }

    public static Response updateUser(Object token, Object organizationId, Object userId, JSONObject body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/users/" + userId);
    }

    public static Response updateCurrentUser(Object token, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put("current");
    }

    public static Response deleteUser(Object token, Object organizationId, Object userId) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .delete(organizationId + "/users/" + userId);
    }

    public static Response getUserById(Object token, Object organizationId, String userId) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/users/" + userId);
    }

    public static Response getUserFullDetailsById(Object token, Object organizationId, Object userId) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/users/" + userId + "/details");
    }

    public static Response uploadUserAvatar(Object token, String filePath, String contentType) {
        final File logo = new File(filePath);
        return given().spec(formDataRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .multiPart("file", logo, contentType)
                .post("image");
    }

    public static Response uploadUserAvatar(Object token, String filePath) {
        String contentType = "image/png";
        return uploadUserAvatar(token, filePath, contentType);
    }

    public static Response uploadUserAvatar(Object token, File file, String contentType) {
        return given()
                .spec(formDataRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .multiPart("file", file, contentType)
                .post("image");
    }

    public static Response deleteUserAvatar(Object token) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .delete("image");
    }

    public static Response expireInvitationToken() {
        final RequestSpecification expireRequestSpecification = new RequestSpecBuilder()
                .setBaseUri("https://scheduler.development.peg.qless.com/api/v1")
                .setBasePath("/TRIGGER_USERS_INVITATIONS_EXPIRATION/trigger")
                .build();

        return given().spec(expireRequestSpecification)
                .get();
    }

    public static Response restoreRequest(Object token, Object organizationId, JSONObject body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/users/restore/request");
    }

    public static Response linkToLocation(Object token, Object organizationId, Object userId, Object locationId, ToggleAction action) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .param("action", action.name())
                .put(organizationId + "/users/" + userId + "/locations/" + locationId + "/toggle-link");
    }

    public static Response searchUsersLinkedLocations(Object token, Object organizationId, Object userId, JSONObject body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/users/" + userId + "/locations/link-aware/search");
    }

    public static Response linkUnlinkUserToLocationService(Object token, Object organizationId, Object userId, Object locationId, Object serviceId, Object action) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .put(organizationId + "/locations/" + locationId + "/users/" + userId + "/services/" + serviceId + "/toggle-link?action=" + action);
    }

    public static Response searchUserLinkedLocationServices(Object token, Object organizationId, Object userId, Object locationId, JSONObject body) {
        return given()
                .spec(orgIdRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/locations/" + locationId + "/users/" + userId + "/services/link-aware/search");
    }

    public static Response setPreferredLocation(String token, String locationId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .put("/current/preferred-location/" + locationId);
    }

    public static Response getUserPreferredBasicLocation(String token) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get("/current/preferred-location/basic");
    }

}
