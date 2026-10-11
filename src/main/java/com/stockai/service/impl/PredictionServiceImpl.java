package com.stockai.service.impl;

import com.stockai.apipridection.service.StockAIService;
import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionFiltersDto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.HistoricalPredictionEntity;
import com.stockai.entity.PredictionEntity;
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
    public List<PredictionDto> getPredictionDetailsByFilters(PredictionFiltersDto filters) {
        List<PredictionEntity> predictionEntities = predictionRepository.findPredictionsByFilters(filters);
        return predictionEntities.stream().map(prediction -> {
            PredictionDto dto = new PredictionDto();
            BeanUtils.copyProperties(prediction, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    private PredictionFiltersDto arrangeFilters(PredictionFiltersDto filters) {
        if (filters == null) {
            return new PredictionFiltersDto();
        }
        return filters;
    }

    @Override
    @Transactional
    public void synchronizePredictions(String startDate, String endDate) {
        log.info("Started saving prediction data");
        predictionRepository.deletePredictionRecords();
        int totalUpserted = 0;
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
            entity.setClose(latest.getClose());
            entity.setVolume((double) latest.getVolume());
            entity.setPer(latest.getPer());
            entity.setPtsC(latest.getPtsC());
            entity.setCreatedDate(latest.getCreatedDate());
            entity.setPredictionDate(LocalDate.now());
            entity.setPredictionClose(latest.getClose());
            entity.setPredictionDirection(analysis.getDirection());
            entity.setPredictionClosePercentage(0.0);
            entity.setPredictedHigh(analysis.getPredictedHigh());
            entity.setPredictedHighPercentage(analysis.getPredictedHighPercentage());
            entity.setPredictedLow(analysis.getPredictedLow());
            entity.setPredictedLowPercentage(analysis.getPredictedLowPercentage());
            entity.setConfidencePercent(analysis.getConfidencePercent());
            entity.setUpProbabilityPercent(analysis.getUpProbabilityPercent());
            entity.setDownProbabilityPercent(analysis.getDownProbabilityPercent());

            predictions.add(entity);
            validationSamples += analysis.getValidationSamples();
            correctValidationPredictions += analysis.getCorrectValidationPredictions();
        }
        log.info("Completed generating predictions for all historical data.");
        double validationAccuracyPercent = validationSamples == 0
                ? 0.0
                : correctValidationPredictions * 100.0 / validationSamples;

        UUID runId = UUID.randomUUID();
        LocalDateTime runCreatedAt = LocalDateTime.now();
        int predictionsSaved = predictions.size();

        for (PredictionEntity prediction : predictions) {
            prediction.setRunId(runId);
            prediction.setRunStartDate(parsedStartDate);
            prediction.setRunEndDate(parsedEndDate);
            prediction.setRunCreatedAt(runCreatedAt);
            prediction.setPredictionsSaved(predictionsSaved);
            prediction.setSkippedSymbols(skippedSymbols);
            prediction.setValidationSamples(validationSamples);
            prediction.setCorrectValidationPredictions(correctValidationPredictions);
            prediction.setValidationAccuracyPercent(validationAccuracyPercent);
            prediction.setAccuracyMetric("Next available session direction; chronological 80/20 holdout");
        }
        log.info("Prepared {} prediction records for saving", predictions.size());
        if (!predictions.isEmpty()) {
            for (int i = 0; i < predictions.size(); i += 500) {
                int end = Math.min(i + 500, predictions.size());
                List<PredictionEntity> batch = predictions.subList(i, end);
                log.info("Saving batch of {} prediction records", batch.size());
                predictionRepository.savePrediction(batch);
                totalUpserted += batch.size();

                List<HistoricalPredictionEntity> historicalBatch = new java.util.ArrayList<>();
                for (PredictionEntity prediction : batch) {
                    HistoricalPredictionEntity historicalEntity = new HistoricalPredictionEntity();
                    BeanUtils.copyProperties(prediction, historicalEntity);
                    historicalEntity.setId(null);
                    historicalBatch.add(historicalEntity);
                }
                log.info("Saving {} historical prediction records for audit trail", historicalBatch.size());
                predictionRepository.saveHistoricalPrediction(historicalBatch);
            }
        }
    }
}
