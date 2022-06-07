package helpers.flows;

import helpers.appsapi.availabletimeslots.AvailableTimeSlotsHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static utils.TestExecutionConstants.SUPPORT_TOKEN;

public class AvailabilityTimeSlotsFlows {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final long NOTICE_TIME_DURATION_IN_HOURS = 1;

    public JSONArray getListOfAvailableTimeSlots(String organizationId, String locationId, String serviceId, JSONObject body) {
        return new JSONArray(AvailableTimeSlotsHelper.getListOfAvailableTimeSlots(SUPPORT_TOKEN, organizationId, locationId, serviceId, body)
                .then()
                .statusCode(SC_OK)
                .extract().asString());
    }

    public List<Map<String, String>> getExpectedTimeslotsForFixedAvailability(JSONObject fixedAvailability, int serviceDuration) {
        final LocalTime availableFrom = LocalTime.parse(fixedAvailability.getString("fromTime"));
        final LocalTime availableTo = LocalTime.parse(fixedAvailability.getString("toTime"));

        final int availabilityInSeconds = (int) Duration.between(availableFrom, availableTo).toSeconds();
        final int amountOfExpectedTimeSlots = availabilityInSeconds / serviceDuration;

        final List<Map<String, String>> timeSlotsForFixedAvailability = new ArrayList<>();
        final LocalDate dateForAvailability = LocalDate.parse(fixedAvailability.getString("date"));

        for (int i = 0; i < amountOfExpectedTimeSlots; i++) {
            final LocalDateTime fromTime = LocalDateTime.of(dateForAvailability, availableFrom.plusSeconds(serviceDuration * i));
            final LocalDateTime toTime = LocalDateTime.of(dateForAvailability, availableFrom.plusSeconds(serviceDuration * (i + 1)));

            if (isNowApplicableForTimeSlot(fromTime, toTime)) {
                final Map<String, String> singleSlot = new HashMap<>();
                final String fromTimeInString = fromTime.format(DATE_TIME_FORMATTER);
                singleSlot.put("from", fromTimeInString);

                final String toTimeInString = toTime.format(DATE_TIME_FORMATTER);
                singleSlot.put("to", toTimeInString);
                timeSlotsForFixedAvailability.add(singleSlot);
            }

        }
        return timeSlotsForFixedAvailability;
    }

    public List<Map<String, String>> getExpectedTimeslotsForSingleRecurringAvailability(JSONObject recurringAvailability, int serviceDuration, String requestedDate) {
        final String availableFromInString = recurringAvailability.getString("fromTime");
        final String availableToInString = recurringAvailability.getString("toTime");
        final DayOfWeek dayOfWeek = DayOfWeek.valueOf(recurringAvailability.getString("dayOfWeek"));
        final LocalTime availableFrom = LocalTime.parse(availableFromInString);
        final LocalTime availableTo = LocalTime.parse(availableToInString);

        final int availabilityInSeconds = (int) Duration.between(availableFrom, availableTo).toSeconds();
        final int amountOfExpectedTimeSlotsInDay = availabilityInSeconds / serviceDuration;
        final List<Map<String, String>> availableTimeSlots = new ArrayList<>();

        final LocalDateTime requestDateForAvailability = LocalDate.parse(requestedDate).atTime(LocalTime.now());
        LocalDateTime dateTimeToStart;

        if (requestDateForAvailability.getYear() == LocalDate.now().getYear() && requestDateForAvailability.getMonth() == LocalDate.now().getMonth()) {
            dateTimeToStart = findNearestDayOfWeekDate(requestDateForAvailability, dayOfWeek);
        } else {
            dateTimeToStart = requestDateForAvailability.with(TemporalAdjusters.firstInMonth(dayOfWeek));
        }

        final LocalDateTime endDate = dateTimeToStart.withDayOfMonth(dateTimeToStart.toLocalDate().lengthOfMonth());
        while (dateTimeToStart.isBefore(endDate)) {

            for (int i = 0; i < amountOfExpectedTimeSlotsInDay; i++) {
                final LocalDateTime fromTime = LocalDateTime.of(dateTimeToStart.toLocalDate(), availableFrom.plusSeconds(serviceDuration * i));
                final LocalDateTime toTime = LocalDateTime.of(dateTimeToStart.toLocalDate(), availableFrom.plusSeconds(serviceDuration * (i + 1)));

                if (isNowApplicableForTimeSlot(fromTime, toTime)) {
                    final Map<String, String> singleSlot = new HashMap<>();
                    final String fromTimeInString = fromTime.format(DATE_TIME_FORMATTER);
                    singleSlot.put("from", fromTimeInString);
                    final String toTimeInString = toTime.format(DATE_TIME_FORMATTER);
                    singleSlot.put("to", toTimeInString);
                    availableTimeSlots.add(singleSlot);
                }
            }
            dateTimeToStart = dateTimeToStart.plusDays(7);
        }
        return availableTimeSlots;
    }

    public List<Map<String, String>> getExpectedTimeslotForWeeklyRecurringAvailability(int serviceDuration, String requestedDate) {
        final String availableFromInString = "09:00";
        final String availableToInString = "19:00";
        final LocalTime availableFrom = LocalTime.parse(availableFromInString);
        final LocalTime availableTo = LocalTime.parse(availableToInString);

        final int availabilityInSeconds = (int) Duration.between(availableFrom, availableTo).toSeconds();
        final int amountOfExpectedTimeSlots = availabilityInSeconds / serviceDuration;
        final List<Map<String, String>> availableTimeSlots = new ArrayList<>();

        final LocalDateTime dateStartingFrom = LocalDate.parse(requestedDate).atTime(LocalTime.now());
        int daysForAvailability;
        LocalDateTime dateTimeToStart;

        if (dateStartingFrom.getYear() == LocalDate.now().getYear()) {
            daysForAvailability = getDaysAmountLeftInMonth(dateStartingFrom.toLocalDate());
            dateTimeToStart = dateStartingFrom;
        } else {
            daysForAvailability = getDayCountOfGivenMonthAndYear(dateStartingFrom.toLocalDate());
            dateTimeToStart = dateStartingFrom.withDayOfMonth(1);
        }

        for (int j = 0; j < daysForAvailability; j++) {
            final LocalDate interestingDate = dateTimeToStart.toLocalDate().plusDays(j);
            for (int i = 0; i < amountOfExpectedTimeSlots; i++) {
                final LocalDateTime fromTime = LocalDateTime.of(interestingDate, availableFrom.plusSeconds(serviceDuration * i));
                final LocalDateTime toTime = LocalDateTime.of(interestingDate, availableFrom.plusSeconds(serviceDuration * (i + 1)));
                if (isNowApplicableForTimeSlot(fromTime, toTime)) {
                    final Map<String, String> singleSlot = new HashMap<>();
                    final String fromTimeInString = fromTime.format(DATE_TIME_FORMATTER);
                    singleSlot.put("from", fromTimeInString);
                    final String toTimeInString = toTime.format(DATE_TIME_FORMATTER);
                    singleSlot.put("to", toTimeInString);
                    availableTimeSlots.add(singleSlot);
                }
            }
        }
        return availableTimeSlots;
    }

    public List<Map<String, String>> getExpectedTimeslotForWeeklyRecurringAndFixedAvailability(JSONObject fixedAvailability, JSONArray recurringAvailabilityForWeek, int serviceDuration, String startingDate) {
        final LocalTime fixedAvailabilityFrom = LocalTime.parse(fixedAvailability.getString("fromTime"));
        final LocalTime fixedAvailabilityTo = LocalTime.parse(fixedAvailability.getString("toTime"));
        final LocalDate fixedAvailabilityDate = LocalDate.parse(fixedAvailability.getString("date"));
        final int fixedAvailabilitySeconds = (int) Duration.between(fixedAvailabilityFrom, fixedAvailabilityTo).toSeconds();
        final int amountOfExpectedTimeSlotsFixedAvailability = fixedAvailabilitySeconds / serviceDuration;

        final String recurringAvailableFromInString = recurringAvailabilityForWeek.getJSONObject(0).getString("fromTime");
        final String recurringAvailableToInString = recurringAvailabilityForWeek.getJSONObject(0).getString("toTime");
        final LocalTime recurringAvailabilityFrom = LocalTime.parse(recurringAvailableFromInString);
        final LocalTime recurringAvailabilityTo = LocalTime.parse(recurringAvailableToInString);
        final int recurringAvailabilitySeconds = (int) Duration.between(recurringAvailabilityFrom, recurringAvailabilityTo).toSeconds();
        final int amountOfExpectedTimeSlots = recurringAvailabilitySeconds / serviceDuration;
        final List<Map<String, String>> availableTimeSlots = new ArrayList<>();

        final LocalDate dateStartingFrom = LocalDate.parse(startingDate);
        int daysForAvailability;
        LocalDate dateToStartCount;

        if (dateStartingFrom.getYear() == LocalDate.now().getYear() && dateStartingFrom.getMonth() == LocalDate.now().getMonth()) {
            daysForAvailability = getDaysAmountLeftInMonth(dateStartingFrom);
            dateToStartCount = dateStartingFrom;
        } else {
            daysForAvailability = getDayCountOfGivenMonthAndYear(dateStartingFrom);
            dateToStartCount = dateStartingFrom.withDayOfMonth(1);
        }

        for (int j = 0; j < daysForAvailability; j++) {
            final LocalDate datesInMonth = dateToStartCount.plusDays(j);
            if (datesInMonth.equals(fixedAvailabilityDate)) {
                for (int i = 0; i < amountOfExpectedTimeSlotsFixedAvailability; i++) {
                    final LocalDateTime fromTime = LocalDateTime.of(datesInMonth, fixedAvailabilityFrom.plusSeconds(serviceDuration * i));
                    final LocalDateTime toTime = LocalDateTime.of(datesInMonth, fixedAvailabilityFrom.plusSeconds(serviceDuration * (i + 1)));
                    if (isNowApplicableForTimeSlot(fromTime, toTime)) {
                        final Map<String, String> singleSlot = new HashMap<>();
                        final String fromTimeInString = fromTime.format(DATE_TIME_FORMATTER);
                        singleSlot.put("from", fromTimeInString);
                        final String toTimeInString = toTime.format(DATE_TIME_FORMATTER);
                        singleSlot.put("to", toTimeInString);
                        availableTimeSlots.add(singleSlot);
                    }
                }
            } else {
                for (int i = 0; i < amountOfExpectedTimeSlots; i++) {
                    final LocalDateTime fromTime = LocalDateTime.of(datesInMonth, recurringAvailabilityFrom.plusSeconds(serviceDuration * i));
                    final LocalDateTime toTime = LocalDateTime.of(datesInMonth, recurringAvailabilityFrom.plusSeconds(serviceDuration * (i + 1)));
                    if (isNowApplicableForTimeSlot(fromTime, toTime)) {
                        final Map<String, String> singleSlot = new HashMap<>();
                        final String fromTimeInString = fromTime.format(DATE_TIME_FORMATTER);
                        singleSlot.put("from", fromTimeInString);
                        final String toTimeInString = toTime.format(DATE_TIME_FORMATTER);
                        singleSlot.put("to", toTimeInString);
                        availableTimeSlots.add(singleSlot);
                    }
                }
            }
        }
        return availableTimeSlots;
    }

    public List<Map<String, String>> getExpectedTimeslotWithAvailabilityAndUnavailableDay(List<Map<String, String>> recurringTimeSlots, LocalDate unavailableDate) {
        final List<Map<String, String>> from = recurringTimeSlots.stream().filter(item -> !LocalDateTime.parse(item.get("from")).toLocalDate().equals(unavailableDate)).collect(Collectors.toList());
        return from;
    }

    public List<Map<String, String>> getExpectedTimeslotWithAvailabilityAndUnavailableDateTime(List<Map<String, String>> recurringTimeSlots, LocalDate unavailableDate, String time1) {
        final List<Map<String, String>> from = recurringTimeSlots.stream().filter(item ->
                !(LocalDateTime.parse(item.get("from")).toLocalDate().equals(unavailableDate) && LocalDateTime.parse(item.get("from")).toLocalTime().toString().equals(time1)))
                .collect(Collectors.toList());
        return from;
    }

    public DayOfWeek getDayOfWeekFromDate(LocalDate date) {
        return date.getDayOfWeek();
    }

    public int getDayCountOfGivenMonthAndYear(LocalDate date) {
        final YearMonth yearMonthObject = YearMonth.of(date.getYear(), date.getMonth());
        return yearMonthObject.lengthOfMonth();
    }

    public int getDaysAmountLeftInMonth(LocalDate date) {
        final LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());
        final int daysBetween = (int) DAYS.between(date, endOfMonth) + 1;
        return daysBetween;
    }

    public int getWeekdayCountTillMonthEnd(LocalDateTime date, DayOfWeek weekday) {
        final LocalDate endOfMonth = date.withDayOfMonth(date.toLocalDate().lengthOfMonth()).toLocalDate();
        LocalDateTime dateToStartCount;

        if (date.getYear() == LocalDate.now().getYear() && date.getMonth() == LocalDate.now().getMonth()) {
            dateToStartCount = date;
        } else {
            dateToStartCount = date.withDayOfMonth(1);
        }

        return numberOfSpecificWeekdayBetween(weekday, dateToStartCount.toLocalDate(), endOfMonth);
    }

    public int numberOfSpecificWeekdayBetween(DayOfWeek day, LocalDate first, LocalDate last) {
        if (last.isBefore(first)) {
            throw new IllegalArgumentException("first " + first + " was after last " + last);
        }
        final LocalDate firstDay = first.with(TemporalAdjusters.next(day));
        final LocalDate lastDay = last.with(TemporalAdjusters.previous(day));
        return (int) ChronoUnit.WEEKS.between(firstDay, lastDay) + 1;
    }

    public void sortTimeslots(List<Map<String, String>> timeSlotList) {
        timeSlotList.sort((timeSlot1, timeSlot2) -> {
            Collection<String> values1 = timeSlot1.values();
            Collection<String> values2 = timeSlot2.values();
            if (!values1.isEmpty() && !values2.isEmpty()) {
                return LocalDateTime.parse(values1.iterator().next()).compareTo(LocalDateTime.parse(values2.iterator().next()));
            } else {
                return 0;
            }
        });
    }

    private LocalDateTime findNearestDayOfWeekDate(LocalDateTime date, DayOfWeek dayOfWeek) {
        while (!date.getDayOfWeek().equals(dayOfWeek)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private Boolean isNowApplicableForTimeSlot(LocalDateTime from, LocalDateTime to) {
        final LocalDateTime now = LocalDateTime.now();
        return !now.plusHours(NOTICE_TIME_DURATION_IN_HOURS).isAfter(from);
    }

}
