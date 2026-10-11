package com.stockai.repository;

import com.stockai.dto.PredictionFiltersDto;
import com.stockai.entity.HistoricalPredictionEntity;
import com.stockai.entity.PredictionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionRepository {

    void savePrediction(List<PredictionEntity> predictionDto);

    // Historical prediction methods
    void saveHistoricalPrediction(List<HistoricalPredictionEntity> predictions);

    List<PredictionEntity> findAllPredictions();

    void deletePredictionRecords();

    List<PredictionEntity> findPredictionsByFilters(PredictionFiltersDto filters);
}
