package e2e.gatewayapps.organizationsresource.data;


import helpers.appsapi.support.organizationsresource.payloads.BlockUnblockOrganizationRequestBody.*;
import helpers.appsapi.support.organizationsresource.payloads.DeleteRestoreOrganizationRequestBody.*;
import org.testng.annotations.DataProvider;

import java.util.Random;

import static helpers.appsapi.support.organizationsresource.payloads.CreateOrganizationRequestBody.Vertical.*;
import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.SortDirection.*;
import static helpers.appsapi.support.organizationsresource.payloads.SearchOrganizationRequestBody.OrganizationSortingKey.*;

public class OrganizationsDataProvider {

    @DataProvider(name = "delete reasons", parallel = true)
    public static Object[][] deleteValidReasons() {
        return new Object[][]{
                {DeleteReasons.LACK_OF_PAYMENT.name()},
                {DeleteReasons.CONTRACT_CANCELLATION.name()},
        };
    }

    @DataProvider(name = "blockReasons")
    public static Object[][] blockReasons() {
        return new Object[][]{
                {BlockReasons.LACK_OF_PAYMENT.name()},
                {BlockReasons.OTHER.name()},
        };
    }

    @DataProvider(name = "invalid reasons", parallel = true)
    public static Object[][] deleteInvalidReasons() {
        return new Object[][]{
                {""},
                {"INVALID_REASON"}
        };
    }

    @DataProvider(name = "valid verticals", parallel = true)
    public static Object[][] organizationValidVerticals() {
        return new Object[][]{
                {GOVERNMENT.name()},
                {EDUCATION.name()},
                {RETAIL_OTHER.name()},
                {HEALTHCARE.name()}
        };
    }

    @DataProvider(name = "organization invalid values", parallel = true)
    public static Object[][] organizationInvalidValues() {
        return new Object[][]{
                {null},
                {true},
                {""}
        };
    }

    @DataProvider(name = "is organization id deleted", parallel = true)
    public static Object[][] organizationIdDeletedValues() {
        return new Object[][]{
                {true},
                {false}
        };
    }


    @DataProvider(name = "valid logo paths", parallel = true)
    public static Object[][] organizationValidLogo() {
        return new Object[][]{
                {"src/test/resources/files/pics/atom-solid.svg"},
                {"src/test/resources/files/pics/charmander.png"},
                {"src/test/resources/files/pics/squirtle.jpeg"}
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

    @DataProvider(name = "valid size")
    static Object[][] validSizeDataProvider() {
        return new Object[][]{
                {1},
                {100},
                {5}
        };
    }

    @DataProvider(name = "valid page")
    static Object[][] validPageDataProvider() {
        return new Object[][]{
                {0},
                {1},
                {10}
        };
    }

    @DataProvider(name = "invalid page")
    static Object[][] invalidPageDataProvider() {
        return new Object[][]{
                {"aaa"},
                {"&&&"}
        };
    }

    @DataProvider(name = "invalid size")
    static Object[][] invalidSizeDataProvider() {
        return new Object[][]{
                {"aaa"},
                {"&&&"},
        };
    }


    @DataProvider(name = "valid deleted flag")
    static Object[][] validDeletedFlag() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    @DataProvider(name = "valid sorting asc params")
    static Object[][] validSortASCParams() {
        return new Object[][]{
                {DELETION_DATE + ":" + ASC},
                {WEBSITE_URL + ":" + ASC},
                {ID + ":" + ASC},
                {NUMBER_OF_LOCATIONS + ":" + ASC},
                {INTERNAL_NAME + ":" + ASC}
        };
    }

    @DataProvider(name = "valid sorting desc params")
    static Object[][] validSortDESCParams() {
        return new Object[][]{
                {DELETION_DATE + ":" + DESC},
                {WEBSITE_URL + ":" + DESC},
                {ID + ":" + DESC},
                {NUMBER_OF_LOCATIONS + ":" + DESC},
                {INTERNAL_NAME + ":" + DESC}
        };
    }

    @DataProvider(name = "invalid sorting params")
    static Object[][] invalidSortParams() {
        return new Object[][]{
                {true},
                {1}
        };
    }

    @DataProvider(name = "invalid sorting enums")
    static Object[][] invalidSortEnums() {
        return new Object[][]{
                {"deletion_date:asc"},
                {"URL:ASC"},
                {"id:DESC"}
        };
    }

}
