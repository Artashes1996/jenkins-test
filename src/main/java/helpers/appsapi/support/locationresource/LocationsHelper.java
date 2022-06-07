package helpers.appsapi.support.locationresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class LocationsHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("support/organizations");

    public static Response createLocation(Object token, JSONObject body, Object organizationId) {
        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/locations");
    }
}
