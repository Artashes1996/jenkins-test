package helpers.appsapi.recurringavailabilitiesresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class RecurringAvailabilitiesHelper extends BaseAPIHelper {

    private final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response createRecurringAvailability(Object token, String organizationId, String locationId, JSONObject body) {

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/locations/" + locationId + "/availabilities/recurring");
    }

    public static Response getRecurringAvailability(Object token, String organizationId, String locationId, String resourceId) {

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .queryParam("resourceId", resourceId)
                .get(organizationId + "/locations/" + locationId + "/availabilities/recurring/list");
    }
}
