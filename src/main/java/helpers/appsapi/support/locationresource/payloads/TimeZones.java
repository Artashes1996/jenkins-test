package helpers.appsapi.support.locationresource.payloads;

import java.util.HashMap;
import java.util.Map;

import static utils.TestUtils.*;

public enum TimeZones {
    ETC_GMT12(-12, "Etc/GMT+12"),
    US_SAMOA(-11, "US/Samoa"),
    US_HAWAII(-10, "US/Hawaii"),
    US_ALASKA(-9, "US/Alaska"),
    AMERICA_VANCOUVER(-8, "America/Vancouver"),
    AMERICA_PHOENIX(-7, "America/Phoenix"),
    PACIFIC_GALAPAGOS(-6, "Pacific/Galapagos"),
    JAMAICA(-5, "Jamaica"),
    AMERICA_CARACAS(-4, "America/Caracas"),
    AMERICA_JUJUY(-3, "America/Jujuy"),
    AMERICA_NORONHA(-2, "America/Noronha"),
    ATLANTIC_CAPE_VERDE(-1, "Atlantic/Cape_Verde"),
    EUROPE_LONDON(0, "Europe/London"),
    EUROPE_ZURICH(1, "Europe/Zurich"),
    EUROPE_K(2, "Europe/Kiev"),
    ASIA_QATAR(3, "Asia/Qatar"),
    ASIA_YEREVAN(4, "Asia/Yerevan"),
    INDIAN_MALDIVES(5, "Indian/Maldives"),
    ASIA_ALMATY(6, "Asia/Almaty"),
    INDIAN_CHRISTMAS(7, "Indian/Christmas"),
    ASIA_SINGAPORE(8, "Asia/Singapore"),
    ASIA_TOKYO(9, "Asia/Tokyo"),
    AUSTRALIA_SYDNEY(10, "Australia/Sydney"),
    PACIFIC_POHNPEI(11, "Pacific/Pohnpei"),
    PACIFIC_FIJI(12, "Pacific/Fiji");

    TimeZones(int utcOffset, String tzDbName) {
        this.utcOffset = utcOffset;
        this.tzDbName = tzDbName;
    }

    private final int utcOffset;
    private final String tzDbName;
    private static final Map<Integer, String> TIME_ZONES = new HashMap<>();

    static {
        for (TimeZones timeZone : TimeZones.values()) {
            TIME_ZONES.put(timeZone.utcOffset, timeZone.tzDbName);
        }
    }

    public static String getTimeZoneByUtcOffset(Integer offset) {
        return TIME_ZONES.get(offset);
    }

    public static Integer getOffsetByTimeZoneName(String timeZoneName) {
        for (Integer i : TIME_ZONES.keySet()) {
            if (TIME_ZONES.get(i).equals(timeZoneName)) {
                return i;
            }
        }
        return null;
    }

    public static String getRandomTimeZoneName() {
        return TimeZones.values()[getRandomInt(TimeZones.values().length)].tzDbName;
    }

}
