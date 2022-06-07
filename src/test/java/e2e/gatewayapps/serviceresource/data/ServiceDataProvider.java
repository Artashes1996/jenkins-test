package e2e.gatewayapps.serviceresource.data;

import configuration.Role;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;

import java.util.Random;

import static helpers.appsapi.servicesresource.payloads.ServiceCreationRequestBody.ResourceSelection.*;

public class ServiceDataProvider {

    @DataProvider(name = "resourceSelection")
    public static Object[][] serviceResourceSelection() {
        return new Object[][]{
                {DISABLED.name()},
                {ALLOWED.name()},
                {REQUIRED.name()}
        };
    }

    @DataProvider(name = "invalidSortValues")
    public static Object[][] invalidSortValues() {
        return new Object[][]{
                {"INTERNAL_NAME"},
                {"DURATION:ASC"},
                {"VISIBILITY:DESC"},
                {"INTERNAL_NAME:desc"},
        };
    }

    @DataProvider(name = "invalid action values")
    public static Object[][] invalidActionValues() {
        return new Object[][]{
                {false},
                {new JSONObject()}
        };
    }

    @DataProvider(name = "invalid location values")
    public static Object[][] invalidLinkValues() {
        return new Object[][]{
                {false},
                {new JSONObject()}
        };
    }

    @DataProvider(name = "invalidFilterType", parallel = true)
    public static Object[][] invalidFilterTypes() {
        final Integer number = new Random().nextInt();
        return new Object[][]{
                {number % 2 == 0},
                {new JSONObject()},
                {new JSONArray()},
                {number}
        };
    }

    @DataProvider(name = "roles with location index", parallel = true)
    public static Object[][] rolesWithLocationIndex() {
        return new Object[][]{
                {Role.SUPPORT, 0},
                {Role.OWNER, 1},
                {Role.ADMIN, 2},
                {Role.LOCATION_ADMIN, 3}
        };
    }

}
