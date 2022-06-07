package helpers.appsapi.fieldsresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class FieldsHelper extends BaseAPIHelper {
    final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response createFields(Object token, Object organizationId, JSONObject body){
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/fields");
    }

    public static Response editField(String token, String organizationId, Integer fieldId, JSONObject body){
        final RequestSpecification requestSpecification = setUpSpec(organizationId, "fields/" + fieldId);

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put();
    }

    public static Response searchFields(Object token, String organizationId, JSONObject body){
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/fields/search");
    }

    public static Response getFieldById(Object token, Object organizationId, Object fieldId){
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/fields/" + fieldId);
    }

    public static Response deleteField(String token, String organizationId, Integer fieldId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .delete(organizationId + "/fields/" + fieldId);
    }

}
