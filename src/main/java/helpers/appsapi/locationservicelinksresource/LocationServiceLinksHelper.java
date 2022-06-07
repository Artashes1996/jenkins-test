package helpers.appsapi.locationservicelinksresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class LocationServiceLinksHelper extends BaseAPIHelper {

    private final static RequestSpecification spec = setUpSpec("/organizations/");

    public static Response linkServiceToGroup(Object token, Object organizationId, Object locationId, Object serviceId, JSONObject body){
        return given()
                .spec(spec)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId + "/locations/" + locationId + "/services/" + serviceId);
    }

    public static Response searchServicesInGroups(Object token, Object organizationId, Object locationId){
        return given()
                .spec(spec)
                .header("Authorization", "Bearer " + token)
                .get(organizationId + "/locations/" + locationId + "/services/tree");
    }
}
