package helpers.appsapi.availabletimeslots;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static helpers.BaseAPIHelper.setUpSpec;
import static io.restassured.RestAssured.given;

public class AvailableTimeSlotsHelper {

    private final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response getListOfAvailableTimeSlots(Object token, String organizationId, String locationId, String serviceId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/locations/" + locationId + "/services/"
                        + serviceId + "/available-time-slots/list");
    }

}