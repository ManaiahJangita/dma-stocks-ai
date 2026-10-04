package com.stockai.service.interf;

import com.stockai.dto.PredictionRunResult;
import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionRunRequest;
import com.stockai.dto.PredictionView;

import java.util.List;
import java.util.UUID;

public interface PredictionService {
    PredictionRunResult savePrediction(String startDate, String endDate);

    PredictionRunResult getLatestRun();

    List<PredictionRunResult> getRecentRuns(PredictionRunRequest filters);

    List<PredictionView> getPredictions(UUID runId, PredictionDto filters);
}
