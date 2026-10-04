package com.stockai.utils;

import com.stockai.dto.InstrumentsDto;
import com.stockai.dto.StockV3Dto;
import com.stockai.entity.DailyStockModel;
import com.stockai.entity.HistoricalDataEntity;
import org.springframework.beans.BeanUtils;
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
                stockModel.setCreated_date(stockDto.getCreatedDate());
                stockModel.setSymbol(stockDto.getSymbol());
                stockModel.setLtP(String.valueOf(stockDto.getClose()));
                stockModel.setTrdVol(String.valueOf(stockDto.getVolume()));
                stockModel.setOpen(String.valueOf(stockDto.getOpen()));
                stockModel.setHigh(String.valueOf(stockDto.getHigh()));
                stockModel.setLow(String.valueOf(stockDto.getLow()));
                stockModel.setDayEndClose(String.valueOf(stockDto.getClose()));
                stockModel.setTrdVol(String.valueOf(stockDto.getVolume()));
                dailyStockModelList.add(stockModel);
            }
        }
        return dailyStockModelList;
    }

}
