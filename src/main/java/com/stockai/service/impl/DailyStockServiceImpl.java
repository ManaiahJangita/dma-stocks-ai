package com.stockai.service.impl;

import com.stockai.dto.StockV3Dto;
import com.stockai.entity.DailyStockModel;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.repository.DailyStockRepository;
import com.stockai.repository.HistoricalDataImpl;
import com.stockai.service.interf.DailyStockService;
import com.stockai.utils.MathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DailyStockServiceImpl implements DailyStockService {
    @Autowired
    private DailyStockRepository dailyStockRepository;

    private static final int DAILY_STOCKS_BATCH_SIZE = 1000;

    @Override
    public void processDailyStockData(List<DailyStockModel> models) {

        log.info("Processing daily stock data for {} records", models.size());
        if (!CollectionUtils.isEmpty(models)) {
            int totalRecords = models.size();
            int batchCount = (int) Math.ceil((double) totalRecords / DAILY_STOCKS_BATCH_SIZE);
            for (int i = 0; i < batchCount; i++) {
                int fromIndex = i * DAILY_STOCKS_BATCH_SIZE;
                int toIndex = Math.min(fromIndex + DAILY_STOCKS_BATCH_SIZE, totalRecords);
                List<DailyStockModel> batchList = models.subList(fromIndex, toIndex);
                dailyStockRepository.saveAllDailyStockData(batchList);
                log.info("Saved batch {} of {} records", i + 1, batchList.size());
            }
        }
        log.info("Saving daily stock data for {} records", models.size());
    }

    @Override
    public int dailyStockServiceCount() {
        return dailyStockRepository.countAll();
    }

    @Override
    public void truncateDailyStockData() {
        log.info("Truncating daily stock data");
        dailyStockRepository.truncateDailyStockData();
    }

    @Override
    public void insertHistoryTable() {
        log.info("Inserting daily stock data into history table sevice impl started");
        List<DailyStockModel> dailyStockModels = dailyStockRepository.fullData();
        log.info("Fetched {} records from daily stock data", dailyStockModels.size());
        List<HistoricalDataEntity> stockV3Dtos = getAllStockV3Dto(dailyStockModels);

        if (!CollectionUtils.isEmpty(stockV3Dtos)) {
            int totalRecords = stockV3Dtos.size();
            int batchCount = (int) Math.ceil((double) totalRecords / DAILY_STOCKS_BATCH_SIZE);
            for (int i = 0; i < batchCount; i++) {
                int fromIndex = i * DAILY_STOCKS_BATCH_SIZE;
                int toIndex = Math.min(fromIndex + DAILY_STOCKS_BATCH_SIZE, totalRecords);
                List<HistoricalDataEntity> batchList = stockV3Dtos.subList(fromIndex, toIndex);
                dailyStockRepository.saveAllHistoricalData(batchList);
                log.info("Inserted batch {} of {} records into history table", i + 1, batchList.size());
            }
        }
    }

    @Override
    public void fullData() {
        List<DailyStockModel> dailyStockModels = dailyStockRepository.fullData();
        log.info("Fetched {} records from daily stock data", dailyStockModels.size());
    }

    private List<HistoricalDataEntity> getAllStockV3Dto(List<DailyStockModel> dailyStockModels) {
        List<HistoricalDataEntity> historicalDataEntities = new ArrayList<>();
        for(DailyStockModel model : dailyStockModels){
            HistoricalDataEntity dto = new HistoricalDataEntity();
            dto.setSymbol(model.getSymbol());
            dto.setOpen(Double.parseDouble(model.getOpen()));
            dto.setHigh(Double.parseDouble(model.getHigh()));
            dto.setLow(Double.parseDouble(model.getLow()));
            dto.setClose(Double.parseDouble(model.getDayEndClose()));
            dto.setVolume(Long.valueOf(model.getTrdVol()));
            dto.setPtsC(MathUtil.roundToTwoDigits(model.getCandleClose() - model.getCandleOpen()));
            double pChange = MathUtil.roundToTwoDigits((dto.getPtsC() / model.getCandleOpen()) * 100);
            dto.setPer(pChange);
            dto.setCreatedDate(model.getCreated_date());
            historicalDataEntities.add(dto);
        }
        return historicalDataEntities;
    }
}
