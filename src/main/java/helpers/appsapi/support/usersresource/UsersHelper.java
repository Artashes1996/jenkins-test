package helpers.appsapi.support.usersresource;

import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class UsersHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("/support/users/");

    public static Response deleteSupportUser(Object token, String supportUserId) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .delete(supportUserId);
    }

}
