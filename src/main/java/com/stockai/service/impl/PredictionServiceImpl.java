package com.stockai.service.impl;

import com.stockai.apipridection.service.StockAIService;
import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionRunRequest;
import com.stockai.dto.PredictionRunResult;
import com.stockai.dto.PredictionView;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.PredictionEntity;
import com.stockai.entity.PredictionRunEntity;
import com.stockai.repository.PredictionRepository;
import com.stockai.service.interf.PredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PredictionServiceImpl implements PredictionService {
    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private StockAIService stockAiService;
    @Autowired
    private PredictionRepository predictionRepository;

    @Override
    @Transactional
    public PredictionRunResult savePrediction(String startDate, String endDate) {
        log.info("Started saving prediction data");

        LocalDate parsedStartDate = LocalDate.parse(startDate);
        LocalDate parsedEndDate = LocalDate.parse(endDate);
        if (parsedStartDate.isAfter(parsedEndDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }

        List<HistoricalDataEntity> historicalData = historicalDataService.getAllHistoricalData(startDate, endDate);
        log.info("Fetched {} historical data records for prediction", historicalData.size());

        Map<String, List<HistoricalDataEntity>> groupedData = historicalData.stream()
                .collect(Collectors.groupingBy(HistoricalDataEntity::getSymbol));

        List<PredictionEntity> predictions = new java.util.ArrayList<>();
        int skippedSymbols = 0;
        int validationSamples = 0;
        int correctValidationPredictions = 0;
        for (Map.Entry<String, List<HistoricalDataEntity>> entry : groupedData.entrySet()) {
            List<HistoricalDataEntity> symbolHistory = entry.getValue();
            symbolHistory.sort(Comparator.comparing(HistoricalDataEntity::getCreatedDate));
            StockAIService.AnalysisResult analysis = stockAiService.analyze(symbolHistory);
            if (analysis == null) {
                skippedSymbols++;
                continue;
            }

            HistoricalDataEntity latest = symbolHistory.get(symbolHistory.size() - 1);
            PredictionEntity entity = new PredictionEntity();
            entity.setSymbol(latest.getSymbol());
            entity.setOpen(latest.getOpen());
            entity.setHigh(latest.getHigh());
            entity.setLow(latest.getLow());
            entity.setDay_end_close(latest.getClose());
            entity.setPrevious_close(symbolHistory.size() > 1
                    ? symbolHistory.get(symbolHistory.size() - 2).getClose()
                    : latest.getClose());
            entity.setTrd_vol((double) latest.getVolume());
            entity.setPer(latest.getPer());
            entity.setPtsC(latest.getPtsC());
            entity.setCreated_date(latest.getCreatedDate());
            entity.setPrediction_date(LocalDate.now().toString());
            entity.setPrediction_close(analysis.getPredictedClose());
            entity.setPrediction_direction(analysis.getDirection());
            entity.setPrediction_close_percentage(analysis.getPredictedClosePercentage());
            entity.setPredicted_high(analysis.getPredictedHigh());
            entity.setPredicted_high_percentage(analysis.getPredictedHighPercentage());
            entity.setPredicted_low(analysis.getPredictedLow());
            entity.setPredicted_low_percentage(analysis.getPredictedLowPercentage());
            entity.setConfidence_percent(analysis.getConfidencePercent());
            entity.setUp_probability_percent(analysis.getUpProbabilityPercent());
            entity.setDown_probability_percent(analysis.getDownProbabilityPercent());

            predictions.add(entity);
            validationSamples += analysis.getValidationSamples();
            correctValidationPredictions += analysis.getCorrectValidationPredictions();
        }
        double validationAccuracyPercent = validationSamples == 0
                ? 0.0
                : correctValidationPredictions * 100.0 / validationSamples;

        PredictionRunEntity run = new PredictionRunEntity();
        run.setStartDate(parsedStartDate);
        run.setEndDate(parsedEndDate);
        run.setCreatedAt(LocalDateTime.now());
        run.setPredictionsSaved(predictions.size());
        run.setSkippedSymbols(skippedSymbols);
        run.setValidationSamples(validationSamples);
        run.setCorrectValidationPredictions(correctValidationPredictions);
        run.setValidationAccuracyPercent(validationAccuracyPercent);
        run.setAccuracyMetric("Next available session direction; chronological 80/20 holdout");
        predictionRepository.savePredictionRun(run);

        predictions.forEach(prediction -> prediction.setRunId(run.getId()));
        if (!predictions.isEmpty()) {
            predictionRepository.savePrediction(predictions);
        }
        return toResult(run);
    }

    @Override
    public PredictionRunResult getLatestRun() {
        PredictionRunEntity run = predictionRepository.findLatestPredictionRun();
        return run == null ? null : toResult(run);
    }

    @Override
    public List<PredictionRunResult> getRecentRuns(PredictionRunRequest filters) {
        PredictionRunEntity entityFilters = new PredictionRunEntity();
        BeanUtils.copyProperties(filters, entityFilters);
        return predictionRepository.findPredictionRuns(entityFilters).stream()
                .map(PredictionServiceImpl::toResult)
                .toList();
    }

    @Override
    public List<PredictionView> getPredictions(UUID runId, PredictionDto filters) {
        PredictionEntity entityFilters = new PredictionEntity();
        BeanUtils.copyProperties(filters, entityFilters);
        return predictionRepository.findPredictions(runId, entityFilters).stream()
                .map(prediction -> new PredictionView(
                        prediction.getId(),
                        prediction.getRunId(),
                        prediction.getSymbol(),
                        prediction.getCreated_date(),
                        LocalDate.parse(prediction.getPrediction_date()),
                        prediction.getDay_end_close(),
                        prediction.getPrediction_close(),
                        prediction.getPredicted_high(),
                        prediction.getPredicted_low(),
                        prediction.getPrediction_direction(),
                        prediction.getConfidence_percent(),
                        prediction.getUp_probability_percent(),
                        prediction.getDown_probability_percent()))
                .toList();
    }

    private static PredictionRunResult toResult(PredictionRunEntity run) {
        return new PredictionRunResult(
                run.getId(),
                run.getStartDate(),
                run.getEndDate(),
                run.getCreatedAt(),
                run.getPredictionsSaved(),
                run.getSkippedSymbols(),
                run.getValidationSamples(),
                run.getCorrectValidationPredictions(),
                run.getValidationAccuracyPercent(),
                run.getAccuracyMetric());
    }

}
