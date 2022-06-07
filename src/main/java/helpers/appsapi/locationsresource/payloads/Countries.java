package helpers.appsapi.locationsresource.payloads;

import lombok.Getter;
import java.util.*;

import static utils.TestUtils.getRandomElementFromList;

public enum Countries {

    UNITED_STATES("United States", "Arizona","America/Juneau (UTC-08:00)"),
    CANADA("Canada", "Quebec", "America/Goose_Bay (UTC-03:00)"),
    AUSTRALIA("Australia", "Victoria", "Australia/Sydney (UTC+10:00)"),
    GERMANY("Germany", null, "Europe/Berlin (UTC+02:00)"),
    BELGIUM("Belgium", null, "Europe/Brussels (UTC+02:00)"),
    JAMAICA("Jamaica", "Saint Mary Parish","America/Jamaica (UTC-05:00)");

    public static String getTimezoneForApi(Countries country){
        return country.getTimeZone().split(" ")[0];
    }

    private static final List<Countries> COUNTRIES =
            Arrays.asList(UNITED_STATES, CANADA, AUSTRALIA, GERMANY, BELGIUM, JAMAICA);

    public static Countries getRandomCountry() {
        return getRandomElementFromList(COUNTRIES);
    }

    @Getter
    private final String country;

    @Getter
    private final String state;

    @Getter
    private final String timeZone;

    Countries(String country, String state, String timeZone) {
        this.country = country;
        this.state = state;
        this.timeZone = timeZone;
    }


}
