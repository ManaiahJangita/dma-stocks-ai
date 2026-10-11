package com.stockai.utils;

import com.stockai.dto.InstrumentsDto;
import com.stockai.dto.StockV3Dto;
import com.stockai.entity.DailyStockModel;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstrumentsMap {

    public static List<InstrumentsDto> instrumentsDto(List<String> instrumentsDtoList) {
        List<InstrumentsDto> instrumentsDto = new ArrayList<>();
        for (String instrument : instrumentsDtoList) {
            InstrumentsDto dto = new InstrumentsDto();
            dto.setSymbol(instrument);
            instrumentsDto.add(dto);
        }
        return instrumentsDto;
    }

    public static List<DailyStockModel> getDailyStockModel(List<StockV3Dto> stocksList) {
        List<DailyStockModel> dailyStockModelList = Collections.emptyList();
        if (!CollectionUtils.isEmpty(stocksList)) {
            dailyStockModelList = new ArrayList<>(stocksList.size());
            for (StockV3Dto stockDto : stocksList) {
                DailyStockModel stockModel = new DailyStockModel();
                double close = stockDto.getClose();
                double previousClose = stockDto.getPreviousClose() != null ? stockDto.getPreviousClose() : close;
                double pointsChange = stockDto.getPointsChange() != null ? stockDto.getPointsChange() : (close - previousClose);
                double percentageChange = stockDto.getPercentageChange() != null ? stockDto.getPercentageChange() : (previousClose > 0 ? ((close - previousClose) / previousClose) * 100 : 0);
                long volume = stockDto.getVolume() != null ? stockDto.getVolume() : 0L;
                long deliveryQuantity = stockDto.getDeliveryQuantity() != null ? stockDto.getDeliveryQuantity() : (stockDto.getDvolume() != null ? stockDto.getDvolume() : (volume > 0 ? Math.round(volume * 0.25) : 0L));
                double deliveryPercent = stockDto.getDeliveryPercent() != null ? stockDto.getDeliveryPercent() : (volume > 0 ? ((deliveryQuantity * 100.0) / volume) : 0);
                double weekHigh = stockDto.getWeekHigh() != null ? stockDto.getWeekHigh() : (stockDto.getWkhi() != null ? stockDto.getWkhi() : close);
                double weekLow = stockDto.getWeekLow() != null ? stockDto.getWeekLow() : (stockDto.getWklo() != null ? stockDto.getWklo() : close);

                stockModel.setCreated_date(stockDto.getCreatedDate());
                stockModel.setSymbol(stockDto.getSymbol());
                stockModel.setLtP(String.valueOf(MathUtil.roundToTwoDigits(close)));
                stockModel.setTrdVol(String.valueOf(volume));
                stockModel.setOpen(String.valueOf(MathUtil.roundToTwoDigits(stockDto.getOpen())));
                stockModel.setHigh(String.valueOf(MathUtil.roundToTwoDigits(stockDto.getHigh())));
                stockModel.setLow(String.valueOf(MathUtil.roundToTwoDigits(stockDto.getLow())));
                stockModel.setDayEndClose(String.valueOf(MathUtil.roundToTwoDigits(close)));
                stockModel.setDeliveryPercent(String.valueOf(MathUtil.roundToTwoDigits(deliveryPercent)));
                stockModel.setDeliveryQuntity(String.valueOf(deliveryQuantity));
                stockModel.setNoOfTrades(String.valueOf(stockDto.getNumberOfTrades() != null ? stockDto.getNumberOfTrades() : volume));
                stockModel.setPer(String.valueOf(MathUtil.roundToTwoDigits(percentageChange)));
                stockModel.setPreviousClose(String.valueOf(MathUtil.roundToTwoDigits(previousClose)));
                stockModel.setPtsC(String.valueOf(MathUtil.roundToTwoDigits(pointsChange)));
                stockModel.setWkhi(String.valueOf(MathUtil.roundToTwoDigits(weekHigh)));
                stockModel.setWklo(String.valueOf(MathUtil.roundToTwoDigits(weekLow)));
                dailyStockModelList.add(stockModel);
            }
        }
        return dailyStockModelList;
    }

}
