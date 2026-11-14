package com.stockai.repository;

import com.stockai.dto.HistoricalPredictionFiltersDto;
import com.stockai.dto.PredictionFiltersDto;
import com.stockai.entity.HistoricalPredictionEntity;
import com.stockai.entity.PredictionEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PredictionRepository {

    void savePrediction(List<PredictionEntity> predictionDto);

    void saveHistoricalPrediction(List<HistoricalPredictionEntity> predictions);

    List<HistoricalPredictionEntity> findHistoricalPredictionDataByFilters(HistoricalPredictionFiltersDto filters);

    List<PredictionEntity> findAllPredictions();

    void truncatePredictionData();

    List<PredictionEntity> findPredictionsByFilters(PredictionFiltersDto filters);

    LocalDate findLatestPredictionDate();

    List<PredictionEntity> findByCreatedDateBetween(LocalDate startDate, LocalDate endDate);

    int countPredictionRecords();
}
