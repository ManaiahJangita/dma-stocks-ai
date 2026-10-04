package com.stockai.data.feed.util;

import com.stockai.client.AngelOneService;
import com.stockai.data.feed.AngelOneRestService;
import com.stockai.data.feed.AngelOneTimeFrameInterval;
import com.stockai.dto.InstrumentsDto;
import com.stockai.dto.StockV3Dto;
import com.stockai.entity.DailyStockModel;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.repository.InstrumentsRepository;
import com.stockai.service.impl.HistoricalDataService;
import com.stockai.service.interf.DailyStockService;
import com.stockai.utils.InstrumentsMap;
import com.stockai.utils.StockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.stockai.utils.InstrumentsMap.getDailyStockModel;
import static com.stockai.utils.InstrumentsMap.instrumentsDto;

@Slf4j
public class ClientHelperMethods {

    public static String updateDailyStockData(InstrumentsRepository instrumentsRepository, DailyStockService dailyStockService, AngelOneService angelOneService) {
        log.info("Started Storing daily stocks data");
        List<String> stockNamesList = instrumentsRepository.findAllInstruments();
        LocalDate lastTradingDay = StockUtil.getLastTradingDay();
        LocalDateTime startDate = LocalDate.now().atStartOfDay();
        List<InstrumentsDto> instruments = InstrumentsMap.instrumentsDto(stockNamesList);
        LocalDateTime endDate = lastTradingDay.atTime(LocalTime.now());
        int count = dailyStockService.dailyStockServiceCount();
        if (count != 0) {
            dailyStockService.truncateDailyStockData();
        }
        for (InstrumentsDto instrument : instruments) {
            List<StockV3Dto> historicalData = angelOneService.getHistoricalData(instrument.getSymbol(), AngelOneTimeFrameInterval.ONE_DAY, startDate, endDate);
            if (!CollectionUtils.isEmpty(historicalData)) {
                saveDailyStockData(getDailyStockModel(historicalData),dailyStockService);
            } else {
                log.warn("Stock {} not found in the angelone hence skipped to import the data.", instrument.getSymbol());
            }
        }
        insertHistoryTable(dailyStockService);
        log.info("Completed Storing daily stocks data in both DailyStocks and DailyStocksHistory tables");
        return "Completed Storing daily stocks data";
    }

    private static void saveDailyStockData(List<DailyStockModel> models,DailyStockService dailyStockService) {
        log.info("Saving daily stock data for {} records", models.size());
        if (!CollectionUtils.isEmpty(models)) {
            dailyStockService.processDailyStockData(models);
            String symbol = models.getFirst().getSymbol();
            log.info("Saved daily stock data for symbol: {}", symbol);
        }
    }

    public static void insertHistoryTable(DailyStockService dailyStockService) {
        log.info("Started Inserting History table DailyStocks");
        dailyStockService.insertHistoryTable();
        log.info("Completed Inserting History table DailyStocks");
    }

    public static void getAllInstrumentsAndSaveHistoricalData(InstrumentsRepository instrumentsRepository, HistoricalDataService historicalDataService, AngelOneRestService angelOneRestService) {
        List<String> instruments = instrumentsRepository.findAllInstruments();
        int count = 0;
        log.info("Saving historical data for {} stocks method execution completed", instruments.size());
        List<InstrumentsDto> instrumentsDto = instrumentsDto(instruments);
        for (InstrumentsDto instrument : instrumentsDto) {
            List<StockV3Dto> stockData = angelOneRestService.getHistoricalData(instrument.getSymbol(), AngelOneTimeFrameInterval.ONE_DAY);
            if (!CollectionUtils.isEmpty(stockData)) {
                historicalDataService.saveHistoricalData(stockData);
                count += stockData.size();
            }
        }
        log.info("Saving historical data for {} stocks method execution completed", count);
    }
}
