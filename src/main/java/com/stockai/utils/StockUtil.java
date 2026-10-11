package com.stockai.utils;

import com.stockai.apiconstants.ApiConstants;
import com.stockai.dto.StockDto;
import com.stockai.dto.StockV2Dto;
import com.stockai.dto.StockV3Dto;
import com.stockai.entity.DailyStockModel;
import com.stockai.enums.CandleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.aspectj.asm.internal.ProgramElement.trim;


public class StockUtil {
    private static final String ALL = "ALL";

    private static final Logger logger = LoggerFactory.getLogger(StockUtil.class);

    private static List<String> tradingHolidays = Collections.emptyList();

    private static final Map<String, List<String>> STOCK_PROPERTIES = new HashMap<>();

    private static final Map<String, String> patternPrefixes = new ConcurrentHashMap<>();

    public static final String DAILY = "";
    public static final String WEEKLY = "Weekly: ";
    public static final String MONTHLY = "Monthly: ";

    //Initialize prefixes
    static {
        patternPrefixes.put(CandleType.DAY.name(), DAILY);
    }

    public static List<String> getPropertyByKey(String... keys) {
        return getPropertyByKey(Arrays.asList(keys));
    }

    public static List<String> getPropertyByKey(List<String> keys) {
        List<String> list = new ArrayList<>();
        if (Objects.nonNull(keys)) {
            for (String key : keys) {
                List<String> list2 = STOCK_PROPERTIES.get(key);
                if (!CollectionUtils.isEmpty(list2)) {
                    list.addAll(list2);
                }
            }
        }

        return list;
    }

    /**
     * Returns last trading date.
     *
     * @return
     */
    public static LocalDate getLastTradingDay() {
        LocalDate lastTradingDay = LocalDate.now();
        while (isWeekdayEnd(lastTradingDay) || isTradingHoliday(lastTradingDay)) {
            lastTradingDay = lastTradingDay.minusDays(1);
        }
        return lastTradingDay;
    }

    /**
     * Returns true if today is trading holiday.
     *
     * @return
     */
    public static boolean isTradingHoliday() {
        return isTradingHoliday(LocalDate.now());
    }

    /**
     * Returns true if today is trading day.
     *
     * @return
     */
    public static boolean isTradingDay() {
        return (!isWeekdayEnd() && !isTradingHoliday()) || isSpecialTradingDay(LocalDate.now());
    }

    /**
     * Returns true if it is trading day on a weekend.
     *
     * @return
     */
    private static boolean isSpecialTradingDay(LocalDate date) {
        String dateString = date.toString();
        boolean isTradingHoliday = false;
        List<String> tradingDays = getPropertyByKey(ApiConstants.EQUITY_TRADING_SPECIAL_WORKING_DAYS_LIST_KEY);
        for (String tradingDay : tradingDays) {
            if (dateString.equalsIgnoreCase(tradingDay.trim())) {
                isTradingHoliday = true;
                break;
            }
        }
        return isTradingHoliday;
    }

    /**
     * Returns true if today is weekend
     *
     * @return
     */
    public static boolean isWeekdayEnd() {
        return isWeekdayEnd(LocalDate.now());
    }

    public static boolean isWeekdayEnd(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return DayOfWeek.SATURDAY.equals(dayOfWeek) || DayOfWeek.SUNDAY.equals(dayOfWeek);
    }

    /**
     * Returns true if given day is trading day.
     *
     * @param date
     * @return
     */
    public static boolean isTradingDay(LocalDate date) {
        return (!isWeekdayEnd(date) && !isTradingHoliday(date)) || isSpecialTradingDay(date);
    }

    /**
     * Returns true if given day is trading holiday.
     *
     * @param date
     * @return
     */
    public static boolean isTradingHoliday(LocalDate date) {
        String dateString = date.toString();
        boolean isTradingHoliday = false;
        for (String holiday : StockUtil.tradingHolidays) {
            if (dateString.equalsIgnoreCase(holiday.trim())) {
                isTradingHoliday = true;
                break;
            }
        }
        return isTradingHoliday;
    }


    public static boolean checkIntradayStartTime() {
        LocalDateTime d = LocalDateTime.now();
        boolean result = false;
        if (d.getHour() >= 9) {
            if (d.getHour() == 9 && d.getMinute() < 15) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }


    public static List<StockDto> getStockDtos(List<DailyStockModel> dailyStocks) {
        List<StockDto> dtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dailyStocks)) {
            for (DailyStockModel model : dailyStocks) {
                StockDto stockDto = new StockDto();
                //BeanUtils.copyProperties(model, stockDto, "");
                stockDto.setCreated_date(LocalDateTime.of(model.getCreated_date(), LocalTime.now()));
                stockDto.setDayEndClose(model.getDayEndClose());
                stockDto.setHigh(model.getHigh());
                stockDto.setLow(model.getLow());
                stockDto.setOpen(model.getOpen());
                stockDto.setSymbol(model.getSymbol());
                stockDto.setTrdVol(model.getTrdVol());
                dtos.add(stockDto);
            }
        }
        return dtos;
    }

    public static List<StockV2Dto> getStockV2Dtos(List<DailyStockModel> dailyStocks) {
        List<StockV2Dto> dtos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dailyStocks)) {
            for (DailyStockModel model : dailyStocks) {
                StockV2Dto stockDto = new StockV2Dto();
                stockDto.setCreatedDate(model.getCreated_date());
                stockDto.setClose(model.getCandleClose());
                stockDto.setHigh(model.getCandleHigh());
                stockDto.setLow(model.getCandleLow());
                stockDto.setOpen(model.getCandleOpen());
                stockDto.setSymbol(model.getSymbol());
                stockDto.setVolume(MathUtil.parseLong(model.getTrdVol()));
                dtos.add(stockDto);
            }
        }
        return dtos;
    }


    /**
     * Returns StockV3Dto from StockDto
     *
     * @param stocksList
     * @return
     */
    public static List<StockV3Dto> getStockV3DtosFromStockDtos(List<StockDto> stocksList) {
        List<StockV3Dto> patternStocks = Collections.emptyList();
        if (!CollectionUtils.isEmpty(stocksList)) {
            patternStocks = new ArrayList<>(stocksList.size());
            for (StockDto stockDto : stocksList) {
                try {
                    StockV3Dto stockV3Dto = new StockV3Dto();
                    if (Objects.nonNull(stockDto.getCreated_date())) {
                        stockV3Dto.setCreatedDate(stockDto.getCreated_date().toLocalDate());
                    }
                    stockV3Dto.setSymbol(stockDto.getSymbol());
                    stockV3Dto.setOpen(stockDto.getCandleOpen());
                    stockV3Dto.setHigh(stockDto.getCandleHigh());
                    stockV3Dto.setLow(stockDto.getCandleLow());
                    stockV3Dto.setClose(stockDto.getCandleClose());
                    stockV3Dto.setPtsC(MathUtil.parseDouble(trim(stockDto.getPtsC())));
                    stockV3Dto.setPer(MathUtil.parseDouble(trim(stockDto.getPer())));
                    stockV3Dto.setVolume(MathUtil.parseLong(trim(stockDto.getTrdVol())));
                    stockV3Dto.setLtP(MathUtil.parseDouble(trim(stockDto.getLtP())));

                    patternStocks.add(stockV3Dto);
                } catch (Exception e) {
                    logger.error("Error while converting stockv3dto model", e);
                }
            }
        }
        return patternStocks;
    }


    public static Double getBuyStopLoss(Double open, double stopLossPercent) {
        Double stopLossValue = open * stopLossPercent;
        Double value = open - stopLossValue;
        return Math.round(value * 100) / 100.0;
    }

    public static Double getDistanceFrom52High(StockDto o1) {
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWkhi())) {
            try {
                double weeksHigh52 = MathUtil.parseDouble(o1.getWkhi());
                if (weeksHigh52 > 0) {
                    double distanceFrom52High = ((weeksHigh52 - o1.getCandleClose()) / weeksHigh52) * 100;
                    return MathUtil.roundToTwoDigits(distanceFrom52High);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return null;
    }

    /**
     * Returns distance from 52weeks high to current close.
     *
     * @param o1
     * @return
     */

    public static Double getDistanceTo52High(StockDto o1, boolean reboundPrice) {
        Double value = null;
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWkhi())) {
            double closePrice = reboundPrice ? o1.getCandleHigh() : o1.getCandleClose();
            try {
                double weeksHigh52 = MathUtil.parseDouble(o1.getWkhi());
                if (weeksHigh52 > 0) {
                    double distanceFrom52High = ((weeksHigh52 - closePrice) / closePrice) * 100;
                    value = MathUtil.roundToTwoDigits(Math.abs(distanceFrom52High));
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return value;
    }

    /**
     * Returns true if given stock close price is lessthan given percentage value to
     * 52week high price.
     *
     * @param o1
     * @return
     */
    public static boolean isSectorMatched(StockDto o1, String sectorSymbol) {
        List<String> propertyByKey = getPropertyByKey(sectorSymbol);
        return ALL.equalsIgnoreCase(sectorSymbol) || Objects.nonNull(o1) && propertyByKey.contains(o1.getSymbol());
    }

    public static boolean checkIntradayEndTime() {
        LocalDateTime d = LocalDateTime.now();
        boolean result = false;
        if (d.getHour() < 16) {
            if (d.getHour() == 15 && d.getMinute() > 30) {
                result = false;
            } else {
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns true if given stock close price is in range for given percentage values to
     * 52week high price.
     *
     * @param o1
     * @return
     */
    public static boolean isDistanceTo52WeeksHighInRange(StockDto o1, double minPer, double maxPer, boolean reboundPrice) {
        Double distanceFrom52High = getDistanceTo52High(o1, reboundPrice);
        return Objects.nonNull(distanceFrom52High) && distanceFrom52High > minPer && distanceFrom52High < maxPer;
    }

    /**
     * Returns true if given stock close price is in range for given percentage values to
     * 52week low price.
     *
     * @param o1
     * @return
     */
    public static boolean isDistanceTo52WeeksLowInRange(StockDto o1, double minPer, double maxPer, boolean reboundPrice) {
        Double distanceTo52Low = getDistanceTo52Low(o1, reboundPrice);
        return Objects.nonNull(distanceTo52Low) && distanceTo52Low > minPer && distanceTo52Low < maxPer;
    }

    /**
     * Returns stock performance from 52weeks low to current close.
     *
     * @param o1
     * @return
     */
    public static double getStockPerformanceFrom52WksLow(StockDto o1) {
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWklo())) {
            try {
                double weeksLow52 = MathUtil.parseDouble(o1.getWklo());
                if (weeksLow52 > 0) {
                    double stockPerformanceFrom52Low = ((o1.getCandleClose() - weeksLow52) / weeksLow52) * 100;
                    return MathUtil.roundToTwoDigits(stockPerformanceFrom52Low);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return -1;
    }

    /**
     * Returns stock performance from 52weeks low to current close.
     *
     * @param o1
     * @return
     */
    public static double getStockPerformanceFrom52WksLow(DailyStockModel o1) {
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWklo())) {
            try {
                double weeksLow52 = MathUtil.parseDouble(o1.getWklo());
                if (weeksLow52 > 0) {
                    double stockPerformanceFrom52Low = ((o1.getCandleClose() - weeksLow52) / weeksLow52) * 100;
                    return MathUtil.roundToTwoDigits(stockPerformanceFrom52Low);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return -1;
    }


    /**
     * Returns distance from 52weeks high to current close.
     *
     * @param o1
     * @return
     */
    public static double getDistanceFrom52Low(StockDto o1, boolean reboundPrice) {
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWklo())) {
            try {
                double candleLow = reboundPrice ? o1.getCandleLow() : o1.getCandleClose();
                double weeksLow = MathUtil.parseDouble(o1.getWklo());
                if (weeksLow > 0) {
                    double distanceTo52Low = ((candleLow - weeksLow) / candleLow) * 100;
                    return MathUtil.roundToTwoDigits(Math.abs(distanceTo52Low));
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return -1;
    }

    /**
     * Returns distance from current close to 52weeks low to current close.
     *
     * @param o1
     * @return
     */
    public static Double getDistanceTo52Low(StockDto o1) {
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWklo())) {
            try {
                double weeksHighLow = MathUtil.parseDouble(o1.getWklo());
                double candleClose = o1.getCandleClose();
                if (weeksHighLow > 0) {
                    double distanceTo52Low = ((candleClose - weeksHighLow) / candleClose) * 100;
                    return MathUtil.roundToTwoDigits(Math.abs(distanceTo52Low));
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return null;
    }

    /**
     * Returns distance from current close to 52weeks low to current close.
     *
     * @param o1
     * @return
     */
    public static Double getDistanceTo52Low(StockDto o1, boolean reboundPrice) {
        if (Objects.nonNull(o1) && Objects.nonNull(o1.getWklo())) {
            if (reboundPrice) {
                try {
                    double weeksHighLow = MathUtil.parseDouble(o1.getWklo());
                    if (weeksHighLow > 0) {
                        double candleLow = o1.getCandleLow();
                        double distanceTo52Low = ((weeksHighLow - candleLow) / candleLow) * 100;
                        return MathUtil.roundToTwoDigits(Math.abs(distanceTo52Low));
                    }
                } catch (Exception e) {
                    // ignore
                }
            } else {
                return getDistanceTo52Low(o1);
            }
        }

        return null;
    }

    /**
     * @param latestCandle
     * @return
     */
    public static double getDayEndPriceOrLtp(DailyStockModel latestCandle) {
        double todayClose = 0;
        if (Objects.nonNull(latestCandle)) {
            Double close = MathUtil.parseDouble(latestCandle.getDayEndClose());
            if (close > 0) {
                todayClose = close;
            } else {
                todayClose = MathUtil.parseDouble(latestCandle.getLtP());
            }
        }
        return todayClose;
    }

    /**
     * Returns candle which has 52 weeks high data.
     * Here assumption that which candle has wkhi data will have wklo dat also.
     *
     * @param history
     * @return
     */
    public static DailyStockModel get52WeeksHiLoCandle(List<DailyStockModel> history) {
        return get52WeeksHiLoCandle(history, history.size());
    }

    /**
     * Returns candle which has 52 weeks high data.
     * Here assumption that which candle has wkhi data will have wklo dat also.
     *
     * @param history
     * @param index
     * @return
     */
    public static DailyStockModel get52WeeksHiLoCandle(List<DailyStockModel> history, int index) {
        DailyStockModel candle = null;
        if (!CollectionUtils.isEmpty(history)) {
            candle = history.get(0);
            for (int i = 0; i < index && i < history.size(); i++) {
                DailyStockModel dailyStockModel = history.get(i);
                if (MathUtil.parseDouble(candle.getWkhi()) < MathUtil.parseDouble(dailyStockModel.getWkhi())) {
                    candle = dailyStockModel;
                }
            }
        }

        return candle;
    }

    /**
     * @param commaSeparatedValue
     */
    public static List<String> split(String commaSeparatedValue, String separator) {
        if (Objects.isNull(separator)) {
            separator = ",";
        }
        List<String> list = new ArrayList<>();
        if (StringUtils.hasText(commaSeparatedValue)) {
            String[] values = commaSeparatedValue.split(separator);
            for (String value : values) {
                list.add(value.trim());
            }
        }

        return list;
    }

}