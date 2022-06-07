package helpers.appsapi.locationsresource.payloads;

import helpers.appsapi.support.locationresource.payloads.CreateLocationRequestBody;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static utils.TestUtils.*;

public class LocationUpdateRequestBody {

    final public Map<EditLocationCombination, Supplier<JSONObject>> MAP = new HashMap<>();
    final public static String ADDRESS = "address";
    final public static String ADDRESS_LINE_1 = "addressLine1";
    final public static String ADDRESS_LINE_2 =  "addressLine2";
    final public static String CITY = "city";
    final public static String COUNTRY = "country";
    final public static String LATITUDE = "latitude";
    final public static String LONGITUDE = "longitude";
    final public static String STATE_REGION = "stateRegion";
    final public static String ZIPCODE = "zipcode";
    final public static String NAME_TRANSLATION = "nameTranslation";
    final public static String DESCRIPTION = "description";
    final public static String INTERNAL_NAME = "internalName";
    final public static String PHONE_NUMBER = "phoneNumber";
    final public static String STATUS = "status";
    final public static String TYPE = "type";

    public enum CitiesEnum {
        CHICAGO,
        HOUSTON,
        PHILADELPHIA,
        PHOENIX,
        ARTIK,
        MARALIK,
        DALLAS,
        AUSTIN,
        JACKSONVILLE,
        INDIANAPOLIS,
        COLUMBUS,
        CHARLOTTE,
        SEATTLE,
        DENVER,
    }

    public enum LocationStatuses {
        ACTIVE,
        INACTIVE
    }

    public enum EditLocationCombination {
        REQUIRED_FIELDS,
        ALL_FIELDS
    }

    private Supplier<JSONObject> requiredFields  = () -> {
        final JSONObject createLocationBody = new JSONObject();
        final JSONObject createAddressBody = new JSONObject();

        createLocationBody.put(STATUS, CreateLocationRequestBody.LocationStatuses.ACTIVE.name());

        createAddressBody.put(ADDRESS_LINE_1, FAKER.address().secondaryAddress());
        createAddressBody.put(CITY, FAKER.address().city());
        createAddressBody.put(COUNTRY, FAKER.address().country());
        createAddressBody.put(ZIPCODE, FAKER.address().zipCode());

        createLocationBody.put(ADDRESS, createAddressBody);
        createLocationBody.put(INTERNAL_NAME, FAKER.lordOfTheRings().location() + " " + getRandomInt());
        createLocationBody.put(NAME_TRANSLATION, FAKER.lordOfTheRings().location());

        return createLocationBody;
    };

    private Supplier<JSONObject> allFields  = () -> {
        final int latitudeMaxValue = 90;
        final int latitudeMinValue = -90;
        final int longitudeMaxValue = 180;
        final int longitudeMinValue = -180;

        final double latitude = RANDOM.nextDouble() * (latitudeMaxValue - latitudeMinValue) + latitudeMinValue;
        final double longitude = RANDOM.nextDouble() * (longitudeMaxValue - longitudeMinValue) + longitudeMinValue;

        final JSONObject createLocationBody = requiredFields.get();

        createLocationBody.put(DESCRIPTION, FAKER.book().title());
        createLocationBody.put(PHONE_NUMBER, "+37477445566");

        createLocationBody.getJSONObject(ADDRESS).put(ADDRESS_LINE_2, FAKER.address().streetAddressNumber());
        createLocationBody.getJSONObject(ADDRESS).put(LATITUDE, latitude);
        createLocationBody.getJSONObject(ADDRESS).put(LONGITUDE, longitude);
        createLocationBody.getJSONObject(ADDRESS).put(STATE_REGION, FAKER.address().state());

        return createLocationBody;
    };

    public JSONObject bodyBuilder(EditLocationCombination combination){
        return MAP.get(combination).get();
    }

    {
        MAP.put(EditLocationCombination.REQUIRED_FIELDS, requiredFields);
        MAP.put(EditLocationCombination.ALL_FIELDS, allFields);
    }
}
