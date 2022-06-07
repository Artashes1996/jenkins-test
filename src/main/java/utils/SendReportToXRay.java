package utils;

import configuration.Config;
import helpers.BaseAPIHelper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class SendReportToXRay extends BaseAPIHelper {

    final static RequestSpecification requestSpecification = new RequestSpecBuilder()
            .setBaseUri(Config.XRAY_URL)
            .build();

    public static Response login() {

        final JSONObject authBody = new JSONObject();
        authBody.put("client_id", Config.XRAY_CLIENT_ID);
        authBody.put("client_secret", Config.XRAY_CLIENT_SECRET);

        return given()
                .spec(requestSpecification)
                .contentType(ContentType.JSON)
                .body(authBody.toString())
                .post("/authenticate");
    }

    public static void main(String[] args) {

        final Map<String,String> query = new HashMap<>();
        query.put("projectKey", "PEG");

        final String filePath =  System.getProperty("user.dir") + "/target/failsafe-reports/testng-results.xml";
        String token = login().getBody().asString();
        token = token.replaceAll("\"","");

        given()
                .spec(requestSpecification)
                .header("Authorization","Bearer " + token)
                .contentType(ContentType.XML)
                .queryParams(query)
                .body(new File(filePath))
                .post("/import/execution/testng");
    }


}
