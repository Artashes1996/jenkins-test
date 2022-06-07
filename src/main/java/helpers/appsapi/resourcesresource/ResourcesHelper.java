package helpers.appsapi.resourcesresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class ResourcesHelper extends BaseAPIHelper {

    public static Response createResource(Object token, String organizationId, JSONObject body) {

        final RequestSpecification requestSpecification = setUpSpec(organizationId, "resources");

        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response getResource(Object token, String organizationId, String resourceId) {

        final RequestSpecification requestSpecification = setUpSpec(organizationId, "resources/" + resourceId);

        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .get();
    }

    public static Response searchAndFilterResources(Object token, String organizationId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec(organizationId, "resources/search");

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response searchLocationWithResourceLinkAware(Object token, String organizationId, String resourceId, JSONObject body) {

        final RequestSpecification requestSpecification = setUpSpec(organizationId, "resources/" +
                resourceId + "/locations/link-aware/search");

        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response updateResource(Object token, String organizationId, String resourceId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec(organizationId, "resources/" +
                resourceId);
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put();
    }

    public static Response locationToResourceToggleLink(Object token, String organizationId, String resourceId, String locationId, String linkUnlink) {
        final RequestSpecification requestSpecification = setUpSpec("/organizations/" + organizationId + "/resources/" +
                resourceId + "/locations/" + locationId + "/toggle-link");
        return given().spec(requestSpecification)
                .queryParam("action", linkUnlink)
                .header("Authorization", "Bearer " + token)
                .put();
    }

    public static Response serviceToResourceToggleLink(Object token, String organizationId, String locationId, String resourceId, String serviceId, String toggleAction) {
        final RequestSpecification requestSpecification = setUpSpec("/organizations/" + organizationId + "/locations/" +
                locationId + "/resources/" + resourceId + "/services/" + serviceId + "/toggle-link");
        return given().spec(requestSpecification)
                .queryParam("action", toggleAction)
                .header("Authorization", "Bearer " + token)
                .put();
    }

    public static Response searchServicesWithLocationResourceLinkAwareness(Object token, String organizationId, String locationId, String resourceId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec("/organizations/" + organizationId + "/locations/" +
                locationId + "/resources/" + resourceId + "/services/link-aware/search");
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

    public static Response searchBasicUserResources(String token, String organizationId, String locationId, JSONObject body) {
        final RequestSpecification requestSpecification = setUpSpec(organizationId, "locations/" + locationId + "/basic-resource-users/search");

        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .post();
    }

}


