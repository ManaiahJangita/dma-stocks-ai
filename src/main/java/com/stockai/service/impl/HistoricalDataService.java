package com.stockai.service.impl;

import com.stockai.dto.StockV3Dto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.repository.HistoricalDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class HistoricalDataService {

    @Autowired
    private HistoricalDataRepository repository;

    private static final int BATCH_SIZE = 600;

    public synchronized void saveHistoricalData(List<StockV3Dto> stockData) {
        int count = 0;
        int lot = 0;
        List<HistoricalDataEntity> entities = getAllHistoricalData(stockData);
        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, entities.size());
            List<HistoricalDataEntity> batch = entities.subList(i, endIndex);
            repository.saveAllHistoricalData(batch);
        }
        log.info("Saving historical data lot {} for {} stocks method execution completed", lot++, entities.size());
        log.info("Saving historical data for {} stocks method execution completed", count);
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
        return repository.findAll(startDate, endDate);
    }
}