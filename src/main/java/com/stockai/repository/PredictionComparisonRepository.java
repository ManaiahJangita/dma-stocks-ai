package com.stockai.repository;

import com.stockai.entity.PredictionComparisonEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PredictionComparisonRepository {

    void saveComparison(List<PredictionComparisonEntity> entities);

    void archiveComparison(List<PredictionComparisonEntity> entities);

    @Query(value = "TRUNCATE TABLE dmastock.prediction_comparison", nativeQuery = true)
    void truncatePredictionComparisonData();

    List<PredictionComparisonEntity> findByPredictionDate(LocalDate predictionDate);

    List<PredictionComparisonEntity> findBySymbolAndPredictionDate(String symbol, LocalDate predictionDate);
}
