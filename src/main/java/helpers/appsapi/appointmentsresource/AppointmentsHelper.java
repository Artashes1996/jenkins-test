package helpers.appsapi.appointmentsresource;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static helpers.BaseAPIHelper.setUpSpec;
import static io.restassured.RestAssured.given;

public class AppointmentsHelper {

    private final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response getListOfLocationServices(Object token, String organizationId, String locationId, JSONObject body) {

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/locations/" + locationId + "/appointments/services/list");
    }

    public static Response getListOfBasicResourceUsers(String token, String organizationId, String locationId, JSONObject searchBody) {

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(searchBody.toString())
                .post(organizationId + "/locations/" + locationId + "/appointments/resources/list");
    }

    public static Response createAppointment(String token, String organizationId, String locationId, String serviceId, JSONObject appointmentCreationBody) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(appointmentCreationBody.toString())
                .post(organizationId + "/locations/" + locationId + "/services/" + serviceId + "/resources/appointments");
    }

    public static Response searchAppointments(String token, String organizationId, String locationId, JSONObject appointmentCreationBody) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(appointmentCreationBody.toString())
                .post(organizationId + "/locations/" + locationId + "/appointments/search");
    }

    public static Response getByConfirmationCode(String organizationId, String locationId, String confirmationCode) {
        return given()
                .spec(requestSpecification)
                .get(organizationId + "/locations/" + locationId + "/appointments/code/" + confirmationCode);
    }

    public static Response getByAppointmentId(String token, String organizationId, String locationId, String appointmentId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/locations/" + locationId + "/appointments/" + appointmentId);
    }

}
