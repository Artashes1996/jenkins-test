package utils;

import com.github.javafaker.Faker;
import helpers.appsapi.fieldsresource.payloads.FieldTypes;
import org.apache.commons.lang.WordUtils;
import org.json.JSONObject;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TestUtils {
    public static final Random RANDOM = new Random();
    public static final Faker FAKER = new Faker();

    public static int getRandomInt(int... bound) {
        if (bound.length != 0) {
            return RANDOM.nextInt(bound[0]);
        }
        return RANDOM.nextInt();
    }

    public static int getRandomInt(int from, int to) {
        return from + RANDOM.nextInt(to - from);
    }

    public static <T extends Enum> T getRandomEnumByClass(Class<T> enumClass) {
        return Arrays.asList(enumClass.getEnumConstants()).get(getRandomInt(enumClass.getEnumConstants().length));
    }

    public static String getRandomPhoneNumber() {
        final int randomNumber = getRandomInt(899999) + 100000;
        return "+37477" + randomNumber;
    }

    public static String capitalize(String text) {
        if (text.contains("RETAIL_OTHER")) {
            return "Retail/Other";
        }
        if (text.contains("_")) {
            String vertical = text.replace("_", " ");
            return WordUtils.capitalize(vertical.toLowerCase());
        }
        return WordUtils.capitalize(text.toLowerCase());
    }

    public static String verticalFormatter(String vertical) {

        if (vertical.contains("_")) {
            vertical = vertical.replace("_", " ");
        }
        vertical = WordUtils.capitalize(vertical.toLowerCase());
        return vertical;
    }

    public static <T> T getRandomElementFromList(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(getRandomInt(list.size()));
    }

    public static List<String> getDaysByYearMonthAndWeekDay(Year year, Month month, String weekday) {
        final ArrayList<String> dates = new ArrayList<>();
        LocalDate date = year.atMonth(month).atDay(1).
                with(TemporalAdjusters.firstInMonth(DayOfWeek.valueOf(weekday)));
        Month mo = date.getMonth();
        while (mo == month) {
            dates.add(String.valueOf(date));
            date = date.with(TemporalAdjusters.next(DayOfWeek.valueOf(weekday)));
            mo = date.getMonth();
        }
        return dates;
    }

}
