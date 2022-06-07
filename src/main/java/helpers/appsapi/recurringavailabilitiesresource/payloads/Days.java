package helpers.appsapi.recurringavailabilitiesresource.payloads;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static utils.TestUtils.*;

public class Days {

    private final static List<String> WEEK_DAYS = Arrays.asList(DayOfWeek.MONDAY.name(), DayOfWeek.TUESDAY.name(), DayOfWeek.WEDNESDAY.name(), DayOfWeek.THURSDAY.name(), DayOfWeek.FRIDAY.name());
    private final static List<String> WEEKEND = Arrays.asList(DayOfWeek.SUNDAY.name(), DayOfWeek.SATURDAY.name());

    public static String getRandomDayName() {
        final List<String> dayNames = Arrays.stream(DayOfWeek.values()).map(DayOfWeek::name).collect(Collectors.toList());
        return dayNames.get(getRandomInt(DayOfWeek.values().length));
    }

    public static DayOfWeek getRandomDay() {
        return DayOfWeek.values()[getRandomInt(DayOfWeek.values().length)];
    }

    public static String getRandomWorkDay() {
        return WEEK_DAYS.get(getRandomInt(WEEK_DAYS.size()));
    }

    public static String getRandomWeekEnd() {
        return WEEKEND.get(getRandomInt(WEEKEND.size()));
    }

}
