package com.stockai.service.interf;

import com.stockai.dto.PredictionComparisonDto;

import java.time.LocalDate;
import java.util.List;

public interface PredictionComparisonService {

    void compareYesterdayPredictionsWithTodaysActuals(LocalDate predictionDate);

    List<PredictionComparisonDto> getComparisonsByPredictionDate(LocalDate predictionDate);

    List<PredictionComparisonDto> getComparisonsBySymbolAndPredictionDate(String symbol, LocalDate predictionDate);

    List<PredictionComparisonDto> getComparisonHistoryByPredictionDate(LocalDate predictionDate);

    List<PredictionComparisonDto> getComparisonHistoryBySymbolAndPredictionDate(String symbol, LocalDate predictionDate);

    void evaluateAndSendBuyAlertIfThresholdReached(LocalDate predictionDate);
}
