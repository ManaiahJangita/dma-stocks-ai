package com.stockai.utils;

import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.util.*;

public class MathUtil {
    /**
     * Rounds the given double value to two decimal places.
     *
     * @param value the double value to be rounded
     * @return the value rounded to two decimal places
     */
    public static Double roundToTwoDigits(double value) {
        return Math.round(value * 100) / 100.0;
    }

    /**
     * Rounds the given double value to two decimal places and the result should
     * be divisible by 5.
     *
     * @param value the double value to be rounded
     * @return the value rounded to two decimal places and divisible by 5
     */
    public static Double roundToTwoDigitsDivisiableBy5(double value) {
        double result = Math.round(value * 5) / 5.0;

        if ((result * 100.0) / 10 != 0) {
            result += 0.05;
            result = roundToTwoDigits(result);
        }
        return result;
    }

    /**
     * Calculate the mean deviation for a given array of numbers.
     *
     * @param numArray the array of numbers
     * @return the mean deviation
     */
    public static double calculateMeanDeviation(double numArray[]) {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }

    /**
     * Calculate the mean deviation for a given list of numbers.
     *
     * @param numArray the list of numbers
     * @return the mean deviation
     */
    public static double calculateMeanDeviation(List<Double> numArray) {
        double sum = 0.0;
        double standardDeviation = 0.0;
        int length = numArray.size();

        for (double num : numArray) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation / length);
    }

    /**
     * Calculate the moving average of a given array of numbers.
     *
     * @param numArray the array of numbers
     * @return the moving average
     */
    public static double calculateMovingAverage(double[] numArray) {
        return calculateMean(numArray);
    }

    /**
     * Calculates the moving average of a given list of numbers.
     *
     * @param closePrices the list of numbers
     * @return the moving average
     */
    public static double calculateMovingAverage(List<Double> closePrices) {
        OptionalDouble average = closePrices.stream().mapToDouble(value -> value).average();
        if (average.isPresent()) {
            return roundToTwoDigits(average.getAsDouble());
        }

        return 0.0;

    }

    /**
     * Calculates the moving average for a specified period from a list of closing prices.
     *
     * @param closePrices the list of closing prices
     * @param period      the number of periods over which to calculate the moving average
     * @return the moving average of the specified period, or 0.0 if the period is non-positive or if the list is empty
     */
    public static double calculateMovingAverage(List<Double> closePrices, int period) {
        if (period <= 0 || closePrices.isEmpty()) {
            return 0.0; // Handle invalid inputs
        }

        int size = closePrices.size();
        int endIndex = Math.min(period, size - 1);
        List<Double> subList = closePrices.subList(0, endIndex);
        OptionalDouble average = subList.stream().mapToDouble(value -> value).average();
        return average.orElse(0.0);
    }

    /**
     * Calculates the mean of a given array of numbers and rounds it to two decimal places.
     *
     * @param numArray the array of numbers
     * @return the mean of the numbers rounded to two decimal places, or 0.0 if the array is empty
     */
    public static double calculateMean(double[] numArray) {
        OptionalDouble average = Arrays.stream(numArray).average();
        if (average.isPresent()) {
            return roundToTwoDigits(average.getAsDouble());
        }

        return 0.0;
    }

    public static Double parseDouble(String value) {
        double result = 0.0;
        try {
            if (StringUtils.hasLength(value)) {
                value = value.replace(",", "").trim();
                result = NumberUtils.parseNumber(value, Double.class);
            }

        } catch (Exception e) {
            // ignore
        }

        return roundToTwoDigits(result);
    }

    /**
     * Converts a given string to a long, returning 0 if the conversion fails.
     * <p>
     * This method will first remove any commas from the given string, then trim
     * it. If the resulting string is empty, 0 will be returned.
     * <p>
     * If the string is not empty, it will be parsed with
     * {@link NumberUtils#parseNumber(String, Class)}. If this fails, 0 will be returned.
     *
     * @param value the value to convert
     * @return the converted value, or 0 if the conversion fails
     */
    public static Long parseLong(String value) {
        Long result = 0L;
        try {
            if (StringUtils.hasLength(value)) {
                value = value.replace(",", "").trim();
                result = NumberUtils.parseNumber(value, Long.class);
            }

        } catch (Exception e) {
            // ignore
        }

        return result;
    }

    /**
     * Converts a given {@link String} to an {@link Integer}.
     *
     * @param value The value to convert.
     * @return The parsed value.
     */
    public static int parseInt(String value) {
        int result = 0;
        try {
            if (StringUtils.hasLength(value)) {
                value = value.replace(",", "").trim();
                result = NumberUtils.parseNumber(value, Integer.class);
            }

        } catch (Exception e) {
            // ignore
        }

        return result;
    }

    /**
     * determine whether two numbers are "approximately equal" by seeing if they are
     * within a certain "tolerance percentage," with `tolerancePercentage` given as
     * a percentage (such as 10.0 meaning "10%").
     *
     * @param desiredValue expected value
     * @param actualValue  actual value
     */
    public static boolean approximatelyEqual(double desiredValue, double actualValue) {
        double diff = Math.abs(desiredValue - actualValue); // 1000 - 950 = 50
        double tolerance = getToerancePercentage(actualValue) / 100 * desiredValue; // 20/100*1000 = 200
        return diff < tolerance; // 50<200 = true
    }

    public static double getToerancePercentage(double value) {
        double tolerancePercentage = 0.3;
        if (value <= 300) {
            tolerancePercentage = 1;
        } else if (value <= 600) {
            tolerancePercentage = 0.9;
        } else if (value <= 1000) {
            tolerancePercentage = 0.7;
        } else if (value <= 2000) {
            tolerancePercentage = 0.5;
        }
        return tolerancePercentage;
    }

    /**
     * determine whether two numbers are "approximately equal" by seeing if they are
     * within a certain "tolerance percentage," with `tolerancePercentage` given as
     * a percentage (such as 10.0 meaning "10%").
     *
     * @param tolerancePercentage 1 = 1%, 2.5 = 2.5%, etc.
     */
    public static boolean approximatelyEqual(double desiredValue, double actualValue, double tolerancePercentage) {
        double diff = Math.abs(desiredValue - actualValue); // 1000 - 950 = 50
        double tolerance = tolerancePercentage / 100 * desiredValue; // 20/100*1000 = 200
        return diff < tolerance; // 50<200 = true
    }

    /**
     * determine whether two numbers are "approximately equal" by seeing if they are
     * within a certain "tolerance percentage," with `tolerancePercentage` given as
     * a percentage (such as 10.0 meaning "10%").
     *
     * @param tolerancePercentage 1 = 1%, 2.5 = 2.5%, etc.
     */
    public static boolean approximatelyLessThanOrEquals(double desiredValue, double actualValue,
                                                        double tolerancePercentage) {
        double diff = Math.abs(desiredValue - actualValue); // 1000 - 950 = 50
        double tolerance = tolerancePercentage / 100 * desiredValue; // 20/100*1000 = 200
        return desiredValue < actualValue || diff < tolerance; // 50<200 = true
    }

    /**
     * determine whether two numbers are "approximately equal" by seeing if they are
     * within a certain "tolerance percentage," with `tolerancePercentage` given as
     * a percentage (such as 10.0 meaning "10%").
     *
     * @param tolerancePercentage 1 = 1%, 2.5 = 2.5%, etc.
     */
    public static boolean approximatelyGreaterThanOrEquals(double desiredValue, double actualValue,
                                                           double tolerancePercentage) {
        double diff = Math.abs(desiredValue - actualValue); // 1000 - 950 = 50
        double tolerance = tolerancePercentage / 100 * desiredValue; // 20/100*1000 = 200
        return desiredValue > actualValue || diff < tolerance; // 50<200 = true
    }

    /**
     * Returns the value in percentage for given actual value.
     *
     * @param actualValue
     * @param percentageValue
     * @return
     */
    public static double getPercentageValue(double actualValue, double percentageValue) {
        double value = percentageValue / 100 * actualValue; // 2/100*1000 = 20
        return roundToTwoDigits(value); // 20
    }

    /**
     * Returns CAGR for given values.
     *
     * @param initialValue
     * @param finalValue
     * @param period
     * @return
     */
    public static double calcualteCAGRValue(double initialValue, double finalValue, int period) {
        double cagr = (Math.pow(finalValue / initialValue, 1.0 / period) - 1.0) * 100;
        return roundToTwoDigits(cagr); // 20
    }

    /**
     * Returns true if value is non-null and greater than zero.
     *
     * @param value - value to check
     * @return true if value is non-null and greater than zero
     */
    public static boolean hasValue(Double value) {
        return Objects.nonNull(value) && value > 0;
    }

    /**
     * Returns value if value is non-null and greater than zero, otherwise returns zero.
     *
     * @param value - value to check
     * @return value if value is non-null and greater than zero, otherwise returns zero
     */
    public static boolean hasValue(Integer value) {
        return Objects.nonNull(value) && value > 0;
    }

    /**
     * Returns value if value is non-null and greater than zero, otherwise returns zero.
     *
     * @param value
     * @return
     */
    public static int getValueOrDefault(Integer value) {
        return Objects.nonNull(value) && value > 0 ? value : 0;
    }

    /**
     * Returns value if value is non-null and greater than zero, otherwise returns null.
     *
     * @param value
     * @return
     */
    public static Double getNullIfZero(Double value) {
        return Objects.nonNull(value) && value > 0 ? value : null;
    }

    /**
     * Calculates the rolling maximum for a given list of values and window size.
     *
     * @param values     List of input values (e.g., high prices)
     * @param windowSize The rolling window size (p)
     * @return List of rolling maximum values
     */
    public static List<Double> calculateRollingMax(List<Double> values, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be greater than 0");
        }

        List<Double> rollingMax = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (i < windowSize - 1) {
                // Add null for indices where the window size cannot be met
                rollingMax.add(null);
            } else {
                // Calculate the max value in the current window
                double max = Double.MIN_VALUE;
                for (int j = i - windowSize + 1; j <= i; j++) {
                    max = Math.max(max, Objects.nonNull(values.get(j)) ? values.get(j) : 0);
                }
                rollingMax.add(max);
            }
        }

        return rollingMax;
    }

    /**
     * Calculates the rolling minimum for a given list of values and window size.
     *
     * @param values     List of input values (e.g., low prices)
     * @param windowSize The rolling window size (p)
     * @return List of rolling minimum values
     */
    public static List<Double> calculateRollingMin(List<Double> values, int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be greater than 0");
        }

        List<Double> rollingMin = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (i < windowSize - 1) {
                // Add null for indices where the window size cannot be met
                rollingMin.add(null);
            } else {
                // Calculate the min value in the current window
                double min = Double.MAX_VALUE;
                for (int j = i - windowSize + 1; j <= i; j++) {
                    min = Math.min(min, Objects.nonNull(values.get(j)) ? values.get(j) : 0);
                }
                rollingMin.add(min);
            }
        }

        return rollingMin;
    }

    public static void main(String args[]) {
        System.out.println("main util.");
        System.out.println(getPercentageValue(100, 1.5));
        System.out.println(getPercentageValue(300, 2));
        System.out.println(roundToTwoDigitsDivisiableBy5(1043.2));

        double range = 5229 - 5140;
        double wick = 5229 - 5191.8;
        double percent = wick * 100 / range;
        System.out.println(percent);

        final double cagrValue = calcualteCAGRValue(10000, 100000, 20);
        System.out.println(cagrValue);
    }

    public static String getNumberNotation(double number) {
        double oneThousand = 1000;
        double oneLakh = 100000;
        double oneCrore = 10000000;
        String notation = "";

        if (number != 0) {
            double absNumber = Math.abs(number); // Use the absolute value for notation calculation

            if (absNumber < oneThousand) {
                notation = String.valueOf(number);
            } else if (absNumber < oneLakh) {
                notation = roundToTwoDigits(absNumber / oneThousand) + "K";
            } else if (absNumber < oneCrore) {
                notation = roundToTwoDigits(absNumber / oneLakh) + "L";
            } else {
                notation = roundToTwoDigits(absNumber / oneCrore) + "Cr";
            }

            // Append a "-" sign if the original number was negative
            if (number < 0) {
                notation = "-" + notation;
            }
        }

        return notation;
    }

    /**
     * Calculate the population standard deviation of a given list of numbers.
     *
     * @param data the list of numbers
     * @return the population standard deviation
     */
    public static double calculateStandardDeviation(List<Double> data) {
        double mean = calculateMean(data);
        double variance = 0.0;

        for (double value : data) {
            variance += Math.pow(value - mean, 2);
        }

        variance /= data.size(); // Population variance
        return Math.sqrt(variance);
    }

    /**
     * Calculate the mean (average) of a given list of numbers.
     *
     * @param data the list of numbers
     * @return the mean of the numbers
     */
    public static double calculateMean(List<Double> data) {
        double sum = 0.0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.size();
    }

    // Function to calculate Correlation
    public static double calculateCorrelation(List<Double> x, List<Double> y) {
        double meanX = calculateMean(x);
        double meanY = calculateMean(y);

        double numerator = 0.0;
        double denominatorX = 0.0;
        double denominatorY = 0.0;

        for (int i = 0; i < x.size(); i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;

            numerator += diffX * diffY;
            denominatorX += Math.pow(diffX, 2);
            denominatorY += Math.pow(diffY, 2);
        }

        double denominator = Math.sqrt(denominatorX * denominatorY);
        return denominator == 0 ? 0 : numerator / denominator;
    }
}
