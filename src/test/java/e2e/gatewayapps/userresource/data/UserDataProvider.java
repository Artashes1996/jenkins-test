package e2e.gatewayapps.userresource.data;

import org.json.JSONObject;
import org.testng.annotations.DataProvider;

public class UserDataProvider {

    @DataProvider(name = "validSize")
    public static Object[][] validSizeDataProvider() {
        return new Object[][]{
                {1},
                {100},
                {5},
                {" "},
                {null}
        };
    }

    @DataProvider(name = "validPage")
    public static Object[][] validPageDataProvider() {
        return new Object[][]{
                {" "},
                {0},
                {2},
                {null}
        };
    }

    @DataProvider(name = "invalidPage")
    public static Object[][] invalidPageDataProvider() {
        return new Object[][]{
                {"aaa"},
                {"&&&"},
                {"true"},
                {new JSONObject()}
        };
    }

    @DataProvider(name = "invalidSize")
    public static Object[][] invalidSizeDataProvider() {
        return new Object[][]{
                {0},
                {"aaa"},
                {"&&&"},
                {101}
        };
    }

    @DataProvider(name = "invalidOrganizationId")
    public static Object[][] invalidOrganizationIdDataProvider() {
        return new Object[][]{
                {"aaa"},
                {"[]"},
                {"&&&"}
        };
    }

    @DataProvider(name = "valid avatar paths")
    public static Object[][] organizationValidLogo() {
        return new Object[][]{
                {"src/test/resources/files/pics/atom-solid.svg"},
                {"src/test/resources/files/pics/squirtle.jpeg"},
                {"src/test/resources/files/pics/charmander.png"},
        };
    }

    @DataProvider(name = "unsupported files", parallel = true)
    public static Object[][] organizationInvalidLogo() {
        return new Object[][]{
                {"src/test/resources/files/pics/frekazoid.gif"},
                {"src/test/resources/files/Captain Marvel.html"},
                {"src/test/resources/files/pics/Mikkky Mauses.bmp"}
        };
    }

    @DataProvider(name = "sorting")
    public static Object[][] sorting() {
        return new Object[][]{
                {"ASC"},
                {"DESC"}
        };
    }

}
