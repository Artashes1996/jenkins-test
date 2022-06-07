package helpers.appsapi.locationservicegroupsresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.*;

public class LocationServiceGroupsHelper extends BaseAPIHelper {

    final static RequestSpecification spec = setUpSpec("/organizations/");

    public static Response createGroup(Object token, Object organizationId, Object locationId, JSONObject groupCreationBody) {
        return given()
                .spec(spec)
                .header("Authorization", "Bearer " + token)
                .body(groupCreationBody.toString())
                .post(organizationId + "/locations/" + locationId + "/location-service-groups");
    }

    public static Response getGroupById(Object token, Object organizationId, Object locationId, Object groupId) {
        return given()
                .spec(spec)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/locations/" + locationId + "/location-service-groups/" + groupId);
    }

    public static Response updateGroup(Object token, Object organizationId, Object locationId, Object serviceGroupId, JSONObject groupUpdateBody) {
        return given()
                .spec(spec)
                .header("Authorization", "Bearer " + token)
                .body(groupUpdateBody.toString())
                .put(organizationId + "/locations/" + locationId + "/location-service-groups/" + serviceGroupId);
    }

}
