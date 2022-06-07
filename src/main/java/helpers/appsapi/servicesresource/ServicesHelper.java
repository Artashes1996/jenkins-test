package helpers.appsapi.servicesresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.*;

public class ServicesHelper extends BaseAPIHelper {

    private final static RequestSpecification requestSpecification = setUpSpec("/organizations/");

    public static Response createService(Object token, String organizationId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/services");
    }

    public static Response sortAndSearchService(String token, String organizationId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/services/search");
    }

    public static Response updateService(String token, String organizationId, String serviceId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/services/" + serviceId);
    }

    public static Response getServiceById(String token, String organizationId, String serviceId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/services/" + serviceId);
    }

    public static Response linkUnlinkLocationsToService(String token, String organizationId, String serviceId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/services/" + serviceId + "/locations/toggle-link");
    }

    public static Response searchForLinkedLocations(String token, String organizationId, String serviceId, JSONObject body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post(organizationId + "/services/" + serviceId + "/locations/link-aware/search");
    }

    public static Response getFieldsLinkedToService(String token, String organizationId, String serviceId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/services/" + serviceId + "/full-service-field-links/list");
    }


}
