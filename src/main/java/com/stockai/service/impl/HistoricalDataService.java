package com.stockai.service.impl;

import com.stockai.dto.StockV3Dto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.InstrumentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class HistoricalDataService {

    @Autowired
    private HistoricalDataRepository repository;

    @Autowired
    private InstrumentsRepository instrumentsRepository;

    private static final int BATCH_SIZE = 600;

    public synchronized void saveHistoricalData(List<StockV3Dto> stockData) {
        log.info("Starting historical data save for {} stock records", stockData == null ? 0 : stockData.size());
        int count = 0;
        int lot = 0;
        List<HistoricalDataEntity> entities = getAllHistoricalData(stockData);
        if (entities == null || entities.isEmpty()) {
            log.warn("No historical data entities created from stock payload; nothing to save");
            return;
        }
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, entities.size());
            List<HistoricalDataEntity> batch = entities.subList(i, endIndex);
            log.debug("Saving historical data batch {} of {} records", (i / BATCH_SIZE) + 1, batch.size());
            repository.saveAllHistoricalData(batch);
        }
        log.info("Historical data save completed for {} records", entities.size());
    }

    public Map<String, List<HistoricalDataEntity>> getHistoryBySymbol(LocalDate startDate, LocalDate endDate) {
        log.info("Loading historical data by date range {} to {} (single-query mode)", startDate, endDate);
        List<HistoricalDataEntity> rows = repository.findAll(startDate, endDate);
        if (rows == null || rows.isEmpty()) {
            log.info("No historical rows found in date range {} to {}", startDate, endDate);
            return new HashMap<>();
        }

        Map<String, List<HistoricalDataEntity>> historyBySymbol = new HashMap<>();
        for (HistoricalDataEntity row : rows) {
            if (row == null || row.getSymbol() == null || row.getSymbol().isBlank()) {
                continue;
            }
            historyBySymbol.computeIfAbsent(row.getSymbol().trim(), key -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<HistoricalDataEntity>> entry : historyBySymbol.entrySet()) {
            entry.getValue().sort(Comparator.comparing(HistoricalDataEntity::getCreatedDate));
        }

        log.info("Completed date-range historical data load. Loaded {} symbols with data from {} to {}",
                historyBySymbol.size(), startDate, endDate);
        return historyBySymbol;
    }

    public Map<String, List<HistoricalDataEntity>> getThreeYearHistoryBySymbol() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(3);
        log.info("Preparing three-year symbol-wise history from {} to {}", startDate, endDate);
        return getHistoryBySymbol(startDate, endDate);
    }

    private List<HistoricalDataEntity> getAllHistoricalData(List<StockV3Dto> dto) {
        log.info("Converting StockV3Dto to HistoricalDataEntity for {} stocks method execution started", dto.size());
        List<HistoricalDataEntity> entities = new ArrayList<>();
        for (StockV3Dto stock : dto) {
            HistoricalDataEntity entity = new HistoricalDataEntity();
            BeanUtils.copyProperties(stock, entity);
            entities.add(entity);
        }
        log.info("Converting StockV3Dto to HistoricalDataEntity for {} stocks method execution completed", entities.size());
        return entities;
    }

    public List<HistoricalDataEntity> getAllHistoricalData(String startDate, String endDate) {
        log.info("Requesting historical data from {} to {}", startDate, endDate);
        return repository.findAll(LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
}