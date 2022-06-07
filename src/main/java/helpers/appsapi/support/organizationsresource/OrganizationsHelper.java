package helpers.appsapi.support.organizationsresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class OrganizationsHelper extends BaseAPIHelper {

    final static RequestSpecification supportRequestSpecification = setUpSpec("/support/organizations/");

    public static Response createOrganization(Object token, JSONObject body) {
        return given().spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response deleteOrganization(Object token, String organizationId, JSONObject body) {
        return given().spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .delete(organizationId);
    }

    public static Response searchOrganizations(Object token, JSONObject body) {
        return given().spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post("search");
    }

    public static Response publishOrganization(Object token, String organizationId) {
        return given()
                .spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .put(organizationId + "/publish");
    }

    public static Response blockOrganization(Object token, String organizationId, JSONObject body) {
        return given()
                .spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/block");
    }

    public static Response unblockOrganization(Object token, String organizationId, JSONObject body) {
        return given()
                .spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/unblock");
    }

    public static Response restoreOrganization(Object token, Object organizationId, JSONObject body) {
        return given().spec(supportRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/restore");
    }
}
