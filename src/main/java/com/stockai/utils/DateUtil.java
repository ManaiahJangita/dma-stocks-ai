package com.stockai.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Map.Entry;

import static java.time.temporal.TemporalAdjusters.previousOrSame;

/**
 * @author sudhakar.badugu
 * Nov 22, 2019
 */
public class DateUtil {

    public static final String YEARLY = "Yearly";
    public static final String MONTHLY = "Monthly";
    public static final String WEEKLY = "Weekly";
    public static final String DAILY = "Daily";

    public static final String MONTH = "MONTH";
    public static final String YEAR = "YEAR";
    public static final String DATE_STR = "DATE";

    private static final Map<Integer, String> MONTHS = new LinkedHashMap<>();

    static {
        MONTHS.put(1, "JAN");
        MONTHS.put(2, "FEB");
        MONTHS.put(3, "MAR");
        MONTHS.put(4, "APR");
        MONTHS.put(5, "MAY");
        MONTHS.put(6, "JUN");
        MONTHS.put(7, "JUL");
        MONTHS.put(8, "AUG");
        MONTHS.put(9, "SEP");
        MONTHS.put(10, "OCT");
        MONTHS.put(11, "NOV");
        MONTHS.put(12, "DEC");
    }

    /**
     * Retrns the this week monday date.
     *
     * @return
     */
    public static LocalDate getMondayDate() {
        return getStartDateByDay(DayOfWeek.MONDAY);
    }

    /**
     * Retrns the this week monday date.
     *
     * @return
     */
    public static LocalDate getWeekStartDate() {
        return getStartDateByDay(DayOfWeek.MONDAY);
    }

    /**
     * Returns the this week start date by given day.
     *
     * @param day
     * @return
     */
    public static LocalDate getStartDateByDay(DayOfWeek day) {
        LocalDate today = LocalDate.now();
        return today.with(previousOrSame(day));
    }

    /**
     * Returns month start date object.
     *
     * @return
     */
    public static LocalDate getMonthStartDate() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Returns month end date object.
     *
     * @return
     */
    public static LocalDate getMonthEndDate() {
        return YearMonth.now().atEndOfMonth();
    }

    /**
     * Returns date for this months last date of Thursday
     *
     * @return
     */
    public static LocalDate getLastThursdayInMonth() {
        return LocalDate.now().with(TemporalAdjusters.lastInMonth(DayOfWeek.THURSDAY));
    }

    /**
     * Returns date for this months last date of Thursday. If date is after expiry with in this month then
     * it returns next month options expiry date.
     *
     * @return
     */
    public static LocalDate getFOExpiryDate() {
        return getFOExpiryDate(LocalDate.now());
    }

    /**
     * Returns next expire dates based on limit parameter.
     * If this month expire date is after expire with in this month then
     * it returns next month options expire date.
     *
     * @return
     */
    public static List<LocalDate> getFOExpiryDates(int j) {
        List<LocalDate> expiryDates = new ArrayList<>();
        LocalDate optionsExpiryDate = getFOExpiryDate(LocalDate.now());
        expiryDates.add(optionsExpiryDate);
        for (int i = 1; i < j; i++) {
            //Find the next expiry date
            optionsExpiryDate = getFOExpiryDate(optionsExpiryDate.plusDays(5));
            expiryDates.add(optionsExpiryDate);
        }

        return Collections.unmodifiableList(expiryDates);
    }

    /**
     * Returns date for this months last date of Thursday. If date is after expiry
     * with in this month then it returns next month options expiry date.
     *
     * @return
     */
    public static LocalDate getFOExpiryDate(LocalDate today) {
        LocalDate lastThursday = getLastThursdayInMonth(today.getMonthValue(), today.getYear())
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.THURSDAY));
        if (today.isAfter(lastThursday)) {
            // Calculate the last Thursday of the next month
            int nextMonth = today.getMonthValue() + 1;
            int nextYear = today.getYear();

            // Adjust year if nextMonth overflows (December)
            if (nextMonth > 12) {
                nextMonth = 1;
                nextYear++;
            }
            lastThursday = getLastThursdayInMonth(nextMonth, nextYear);
        }

        // If last Thursday is holiday then get previous Thursday
        if (StockUtil.isTradingHoliday(lastThursday)) {
            lastThursday = lastThursday.minusDays(1);
        }
        return lastThursday;
    }

    /**
     * Returns date for last date of Thursday of in given month and year
     *
     * @param month
     * @param year
     * @return
     */
    public static LocalDate getLastThursdayInMonth(int month, int year) {
        return LocalDate.of(year, month, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.THURSDAY));
    }

    /**
     * @param date
     * @param url
     * @return
     */
    public static String replaceDatePlaceHolderWithValues(LocalDate date, String url) {
        return replaceDatePlaceHolderWithValues(date, url, true, true);
    }

    /**
     * @param date
     * @param url
     * @return
     */
    public static String replaceDatePlaceHolderWithValues(LocalDate date, String url, boolean replaceFullYear, boolean replaceWithMonthName) {
        if (replaceFullYear) {
            url = url.replaceAll(YEAR, String.valueOf(date.getYear()));
        } else {
            String shortYear = String.valueOf(date.getYear()).substring(2);
            url = url.replaceAll(YEAR, shortYear);
        }

        if (replaceWithMonthName) {
            url = url.replaceAll(MONTH, MONTHS.get(date.getMonthValue()));
        } else {
            String monthValue = "" + (date.getMonthValue() < 10 ? "0" + date.getMonthValue() : date.getMonthValue());
            url = url.replaceAll(MONTH, monthValue);
        }
        String dayOfMonth = "" + (date.getDayOfMonth() < 10 ? "0" + date.getDayOfMonth() : date.getDayOfMonth());
        url = url.replaceAll(DATE_STR, dayOfMonth);
        return url;
    }

    /**
     * Returns year in string format.
     *
     * @param date
     * @param isShortYear
     * @return
     */
    public static String getYearAsString(LocalDate date, boolean isShortYear) {
        String yearStr = String.valueOf(date.getYear());
        if (isShortYear) {
            yearStr = String.valueOf(date.getYear()).substring(2);
        }

        return yearStr;
    }

    /**
     * Returns year in string format.
     *
     * @param date
     * @return
     */
    public static String getMonthAsString(LocalDate date) {
        return MONTHS.get(date.getMonthValue());
    }

    /**
     * Returns year in string format.
     *
     * @param monthName
     * @param year
     * @return
     */
    public static LocalDate getDateFromMonthAndYear(String monthName, int year) {
        int monthNumber = 1;
        for (Entry<Integer, String> entry : MONTHS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(monthName)) {
                monthNumber = entry.getKey();
                break;
            }
        }
        return LocalDate.of(year, monthNumber, 1);
    }

    /**
     * Converts the given date to {@link LocalDateTime}. If parameter is null then
     * it returns current time.
     *
     * @param dateToConvert - null then returns current time.
     * @return
     */
    public static LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        LocalDateTime time = LocalDateTime.now();
        if (Objects.nonNull(dateToConvert)) {
            try {
                time = dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception e) {
            }
        }

        return time;
    }

    public static Date convertToDateViaInstant(LocalDateTime dateToConvert) {
        if (Objects.isNull(dateToConvert)) {
            dateToConvert = LocalDateTime.now();
        }

        return java.util.Date
                .from(dateToConvert.atZone(ZoneId.systemDefault())
                        .toInstant());
    }

    public static Date convertToDateViaInstant(LocalDate dateToConvert) {
        if (Objects.isNull(dateToConvert)) {
            dateToConvert = LocalDate.now();
        }

        return java.util.Date
                .from(dateToConvert.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts the given date to {@link LocalDate}. If parameter is null then it
     * returns current date.
     *
     * @param dateToConvert
     * @return
     */
    public static LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        LocalDate date = LocalDate.now();
        if (Objects.nonNull(dateToConvert)) {
            try {
                date = dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception e) {
            }
        }

        return date;
    }

    /**
     * Converts the given date to {@link LocalDateTime}. If parameter is null then
     * it returns current time.
     *
     * @param dateToConvert
     * @return
     */
    public static LocalDateTime convertToLocalDateTimeViaMilisecond(Date dateToConvert) {
        LocalDateTime time = LocalDateTime.now();
        if (Objects.nonNull(dateToConvert)) {
            try {
                time = Instant.ofEpochMilli(dateToConvert.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception e) {
            }
        }

        return time;
    }

    /**
     * Returns true if day is matched with given day
     *
     * @param day -
     * @return
     */
    public static boolean isDayMatched(int day) {
        Calendar myDate = Calendar.getInstance(); // set this up however you need it.
        int dow = myDate.get(Calendar.DAY_OF_WEEK);
        return (dow == day);
    }

    public static LocalDate getDateByRangeType(String rangeType) {
        LocalDate date = LocalDate.now();
        if (WEEKLY.equalsIgnoreCase(rangeType)) {
            date = date.minusWeeks(1);
        } else if (MONTHLY.equalsIgnoreCase(rangeType)) {
            date = date.minusMonths(1);
        } else if (YEARLY.equalsIgnoreCase(rangeType)) {
            date = date.minusYears(1);
        }
        return date;
    }

    public static LocalDate getDateByRangeType(String rangeType, long dateToSubtract) {
        return getDateByRangeType(LocalDate.now(), rangeType, dateToSubtract);
    }

    public static LocalDate getDateByRangeType(LocalDate date, String rangeType, long dateToSubtract) {
        if (Objects.isNull(date)) {
            date = LocalDate.now();
        }

        if (DAILY.equalsIgnoreCase(rangeType)) {
            date = date.minusDays(dateToSubtract);
        } else if (WEEKLY.equalsIgnoreCase(rangeType)) {
            date = date.minusWeeks(dateToSubtract);
        } else if (MONTHLY.equalsIgnoreCase(rangeType)) {
            date = date.minusMonths(dateToSubtract);
        } else if (YEARLY.equalsIgnoreCase(rangeType)) {
            date = date.minusYears(dateToSubtract);
        }
        return date;
    }

    public static void main(String[] args) {
        List<LocalDate> optionsExpiryDate2 = getFOExpiryDates(3);
        System.out.println(optionsExpiryDate2);

        String date = "2021-03-02T00:00:00+05:30";
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE_TIME);
        System.out.println(localDate);
    }
}