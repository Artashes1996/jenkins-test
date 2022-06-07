package helpers.appsapi.fixedavailabilitiesresource;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static helpers.BaseAPIHelper.setUpSpec;
import static io.restassured.RestAssured.given;

public class FixedAvailabilitiesHelper {

    private final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response createFixedAvailability(Object token, String organizationId, String locationId, JSONObject body) {

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/locations/" + locationId + "/availabilities/fixed");
    }

    public static Response searchFixedAvailabilities(Object token, String organizationId, String locationId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec(organizationId, "locations/" + locationId + "/availabilities/fixed/list");

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response makeDayUnavailable(Object token, String organizationId, String locationId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec(organizationId, "locations/" + locationId + "/availabilities/fixed/make-unavailable");

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put();
    }

}
