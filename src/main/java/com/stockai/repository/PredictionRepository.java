package com.stockai.repository;

import com.stockai.entity.PredictionEntity;
import com.stockai.entity.PredictionRunEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface PredictionRepository {

    public void savePrediction(List<PredictionEntity> predictionDto);

    List<PredictionEntity> findAllPredictions();

    void savePredictionRun(PredictionRunEntity predictionRun);

    PredictionRunEntity findLatestPredictionRun();

    List<PredictionRunEntity> findPredictionRuns(PredictionRunEntity filters);

    List<PredictionEntity> findPredictions(UUID runId, PredictionEntity filters);
}
