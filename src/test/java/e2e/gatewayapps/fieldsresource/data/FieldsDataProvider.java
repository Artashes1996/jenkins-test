package e2e.gatewayapps.fieldsresource.data;

import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import org.testng.annotations.DataProvider;

public class FieldsDataProvider {

    @DataProvider(name = "validFieldTypes")
    public static Object[][] fieldTypeAll() {
        return new Object[][]{
                {FieldTypes.TEXT},
                {FieldTypes.NUMBER},
                {FieldTypes.CHECKBOX},
                {FieldTypes.RADIOBUTTON},
                {FieldTypes.MULTI_SELECT_DROPDOWN},
                {FieldTypes.SINGLE_SELECT_DROPDOWN}
        };
    }

    @DataProvider(name = "validFieldTypesWithOptions")
    public static Object[][] fieldTypeWithOptions() {
        return new Object[][]{
                {FieldTypes.CHECKBOX},
                {FieldTypes.RADIOBUTTON},
                {FieldTypes.MULTI_SELECT_DROPDOWN},
                {FieldTypes.SINGLE_SELECT_DROPDOWN}
        };
    }

    @DataProvider(name = "validFieldTypesWithoutOptions")
    public static Object[][] fieldTypeWithoutOptions() {
        return new Object[][]{
                {FieldTypes.TEXT},
                {FieldTypes.NUMBER}
        };
    }

    @DataProvider(name = "invalid Type")
    public static Object[][] fieldTypeInvalidData() {
        return new Object[][]{
                {"invalid"},
                {null},
                {""},
                {-1}
        };
    }

    @DataProvider(name = "invalid page")
    public static Object[][] invalidPageData() {
        return new Object[][]{
                {"aaa"},
                {"&&&"}
        };
    }

    @DataProvider(name = "invalid size")
    public static Object[][] invalidSizeDataProvider() {
        return new Object[][]{
                {0},
                {101},
                {-1}
        };
    }

    @DataProvider(name = "invalid sort")
    public static Object[][] invalidSortDataProvider() {
        return new Object[][]{
                {"ID:ASC"},
                {"&&&"},
                {"FIELD_TYPE:DESC"},
                {"id:DEsc"}
        };
    }

    @DataProvider(name = "validRegex")
    public static Object[][] validRegex() {
        return new Object[][]{
                {"[ab]{4,6}c"},
                {null},
                {"[^0-9]*[12]?[0-9]{1,2}[^0-9]*"}
        };
    }
}
