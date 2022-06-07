package helpers.appsapi.organizationsresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;

import static io.restassured.RestAssured.*;

public class OrganizationsHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("/organizations/");
    final static RequestSpecification formDataRequestSpecification = setUpSpec("/organizations/").contentType("multipart/form-data");

    public static Response getOrganizationById(Object token, String organizationId, Boolean includeDeleted) {
        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .formParam("includeDeleted", includeDeleted)
                .get(organizationId);
    }

    public static Response uploadOrganizationImage(Object token, Object organizationId, String filePath, String contentType) {
        final File logo = new File(filePath);
        return given().spec(formDataRequestSpecification)
                .header("Authorization", "Bearer " + token)
                .multiPart("file", logo, contentType)
                .post(organizationId + "/image/upload");
    }

    public static Response deleteOrganizationImage(Object token, Object organizationId) {
        return given().spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .delete(organizationId + "/image");
    }

    public static Response pauseOrganization(Object token, Object organizationId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .put(organizationId + "/pause");
    }

    public static Response unpauseOrganization(Object token, Object organizationId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .put(organizationId + "/unpause");
    }

    public static Response updateOrganization(Object token, Object organizationId, Object body) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .put(organizationId.toString());
    }

}
