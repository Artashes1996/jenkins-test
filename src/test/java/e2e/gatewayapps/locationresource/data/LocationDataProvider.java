package e2e.gatewayapps.locationresource.data;

import helpers.appsapi.locationsresource.payloads.LocationsSearchRequestBody;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;

import static helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody.*;

public class LocationDataProvider {
    @DataProvider(name = "missRequiredAddressFields")
    protected static Object[][] invalidAddressFieldsValues() {

        final JSONObject mapWithAllValues = new JSONObject() {{
            put(ADDRESS_LINE_1, "Address new line 1 any");
            put(ADDRESS_LINE_1, "Address any 2");
            put(CITY, "Gyumri");
            put(COUNTRY, "Armenia");
            put(LATITUDE, 12);
            put(LONGITUDE, 13);
            put(STATE_REGION, "Yerevan/Armenia");
            put(ZIPCODE, "0123");
        }};

        final JSONObject mapMissingAddress;
        mapMissingAddress = mapWithAllValues;
        mapMissingAddress.put(ADDRESS_LINE_1, JSONObject.NULL);
        final JSONObject mapMissingCity;
        mapMissingCity = mapWithAllValues;
        mapMissingCity.put(CITY, JSONObject.NULL);
        final JSONObject mapMissingCountry;
        mapMissingCountry = mapWithAllValues;
        mapMissingCountry.put(COUNTRY, JSONObject.NULL);
        final JSONObject mapMissingZipcode;
        mapMissingZipcode = mapWithAllValues;
        mapMissingZipcode.put(ZIPCODE, JSONObject.NULL);

        return new Object[][]{
                {mapMissingAddress},
                {mapMissingCity},
                {mapMissingCountry},
                {mapMissingZipcode},
        };
    }

    @DataProvider(name = "address required fields")
    protected static Object[][] allAddressFields() {
        return new Object[][]{
                {ADDRESS_LINE_1},
                {CITY},
                {COUNTRY},
                {ZIPCODE},
        };
    }

    @DataProvider(name = "invalid Timezone Values")
    protected static Object[][] invalidTimezoneValues() {
        return new Object[][]{
                {"Asia - Yerevan"},
                {12},
                {"Asia"},
                {"Asia / Yerevan"},
                {"Asia//Yerevan"},
                {true},
        };
    }

    @DataProvider(name = "location invalid status", parallel = true)
    protected static Object[][] invalidStatuses() {
        return new Object[][]{
                {1},
                {null},
                {""},
                {"AKTIF"}
        };
    }

    @DataProvider(name = "locationSearchModes", parallel = true)
    public static Object[][] locationSearchModes() {
        return new Object[][]{
                {LocationsSearchRequestBody.LocationSearchModes.NO_FILTERING},
                {LocationsSearchRequestBody.LocationSearchModes.FILTERED_BY_SEARCH_LOCATIONS_PERMISSION}
        };
    }

}
