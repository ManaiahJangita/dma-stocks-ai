package com.stockai.repository;

import com.stockai.entity.HistoricalDataEntity;

import java.time.LocalDate;
import java.util.List;

public interface HistoricalDataRepository {
    void saveAllHistoricalData(List<HistoricalDataEntity> historicalDataEntities);

    List<HistoricalDataEntity> findAll(LocalDate startDate, LocalDate endDate);

    LocalDate findLatestCreatedDate();

}
