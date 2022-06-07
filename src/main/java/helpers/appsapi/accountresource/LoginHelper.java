package helpers.appsapi.accountresource;

import configuration.Role;
import configuration.User;
import helpers.BaseAPIHelper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

public class LoginHelper extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = setUpSpec("/accounts");

    public static Response login( JSONObject body) {

        return given()
                .spec(requestSpecification)
                .body(body.toString())
                .post("/login");
    }


    public static Response login(Role role) {

        final User user = new User(role);
        final JSONObject loginBody = new JSONObject();

        loginBody.put("email", user.getEmail());
        loginBody.put("password", user.getPassword());

        return given()
                .spec(requestSpecification)
                .body(loginBody.toString())
                .post("/login");
    }

    public static Response login( Object username, Object password ) {

        final JSONObject loginBody = new JSONObject();

        loginBody.put("email", username);
        loginBody.put("password", password);

        return given()
                .spec(requestSpecification)
                .body(loginBody.toString())
                .post("/login");
    }

    public static Response logout(String token) {
        return given()
                .spec(requestSpecification)
                .header("Authorization", "Bearer " + token)
                .post("/logout");

    }
}
