package com.stockai.repository;

import com.stockai.entity.HistoricalDataEntity;

import java.util.List;

public interface HistoricalDataRepository {
    void saveAllHistoricalData(List<HistoricalDataEntity> historicalDataEntities);

    List<HistoricalDataEntity> findAll(String startDate, String endDate);
}
