package helpers;

import configuration.Config;
import io.restassured.authentication.BasicAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.from;
import static org.apache.http.HttpStatus.SC_OK;

public class SauceLabsHelper {

    final static BasicAuthScheme basicAuthScheme = new BasicAuthScheme();

    static {
        basicAuthScheme.setPassword(Config.SAUCE_LABS_KEY);
        basicAuthScheme.setUserName(Config.SAUCE_LABS_USER);
    }

    final static RequestSpecification requestSpecification = new RequestSpecBuilder()
            .setBaseUri(Config.SAUCE_LABS_REST_API)
            .setBasePath("/" + Config.SAUCE_LABS_USER + "/tunnels")
            .setAuth(basicAuthScheme)
            .build();

    public static List<String> getAllTunnelsForUser(){

        return from(given()
                .spec(requestSpecification)
                .get()
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract()
                .body().asString()).getList("");
    }

    public static boolean isTunnelRunning(String tunnelIdentifier){
        final String tunnelId = getFirstTunnelIdByTunnelIdentifier(tunnelIdentifier);
        return given()
                .spec(requestSpecification)
                .get("/" + tunnelId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("status").toString().equals("running");
    }

    @SneakyThrows
    public static String getFirstTunnelIdByTunnelIdentifier(String tunnelIdentifier) {

        int maxAttempts = 2;
        while(getAllTunnelsForUser() == null && maxAttempts > 0) {
            Thread.sleep(5000);
            maxAttempts --;
        }
        for (final String tunnel : Objects.requireNonNull(getAllTunnelsForUser())) {
            final String item = given()
                    .spec(requestSpecification)
                    .get("/" + tunnel)
                    .then()
                    .extract()
                    .body().asString();
            if (item.contains(tunnelIdentifier)) {
                return from(item).getString("id");
            }
        }
        return null;
    }

    public static void deleteTunnel(String tunnelIdentifier) {
        given()
                .spec(requestSpecification)
                .delete("/" + getFirstTunnelIdByTunnelIdentifier(tunnelIdentifier))
                .then()
                .statusCode(SC_OK);
    }

}
