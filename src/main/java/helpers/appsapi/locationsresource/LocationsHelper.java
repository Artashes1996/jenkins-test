package helpers.appsapi.locationsresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.*;

public class LocationsHelper extends BaseAPIHelper {
    final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response searchLocation(Object token, String organizationId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/locations/search");
    }

    public static Response getLocation(Object token, String organizationId, String locationId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/locations/" + locationId);
    }

    public static Response updateLocation(Object token, Object organizationId, String locationId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/locations/" + locationId);
    }

    public static Response changeLocationsStatuses(Object token, Object organizationId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/locations/change-statuses");
    }

    public static Response linkUnlinkServicesToLocation(Object token, String organizationId, String locationId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec("/organizations/" + organizationId +
                "/locations/" + locationId + "/toggle-link");
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put();
    }

    public static Response searchUserLinkedLocation(String token, String organizationId, String userId, JSONObject body) {
       return given().
               spec(requestSpecification)
               .header("Authorization", "Bearer " + token)
               .body(body.toString())
               .post(organizationId + "/locations/linked-to-user/"+ userId + "/basic/search");
    }

}