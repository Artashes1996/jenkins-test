package helpers;

import configuration.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

import static io.restassured.http.ContentType.JSON;

public class BaseAPIHelper {
    static final String BASE_URI = Config.URI;

    public static RequestSpecification setUpSpec(String basePath) {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setBasePath(basePath)
                .setContentType(JSON)
                .build();
    }

    public static RequestSpecification setUpSpec(String organizationId, String path) {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setBasePath("/organizations/" + organizationId + "/" + path)
                .setContentType(JSON)
                .build();
    }

}
