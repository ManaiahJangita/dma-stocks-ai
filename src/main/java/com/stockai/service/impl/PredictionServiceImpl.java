package com.stockai.service.impl;

import com.stockai.apipridection.service.StockAIService;
import com.stockai.dto.HistoricalPredictionFiltersDto;
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
import java.util.*;
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
        log.info("[PredictionService] Fetching prediction records with filters={}", filters);
        try {
            List<PredictionEntity> predictionEntities = predictionRepository.findPredictionsByFilters(filters);
            log.info("[PredictionService] Repository returned {} prediction rows for filters={}",
                    predictionEntities == null ? 0 : predictionEntities.size(), filters);

            List<PredictionDto> response = predictionEntities.stream().map(prediction -> {
                PredictionDto dto = new PredictionDto();
                BeanUtils.copyProperties(prediction, dto);
                return dto;
            }).collect(Collectors.toList());

            log.info("[PredictionService] Mapped {} prediction records to DTOs", response.size());
            return response;
        } catch (Exception e) {
            log.error("[PredictionService] Failed to fetch predictions with filters={}", filters, e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public void synchronizePredictions(String startDate, String endDate) {
        log.info("[PredictionService] Starting prediction synchronization. startDate={}, endDate={}", startDate, endDate);
        int count = predictionRepository.countPredictionRecords();
        log.info("[PredictionService] Existing prediction table row count before sync={}", count);
        try {
            log.info("[PredictionService] Truncating existing prediction data before recalculating predictions");
            predictionRepository.truncatePredictionData();
            log.info("[PredictionService] Existing prediction data truncated successfully");
        } catch (Exception e) {
            log.error("[PredictionService] Failed to truncate existing prediction data before sync", e);
            throw e;
        }

        int totalUpserted = 0;
        log.info("[PredictionService] Loading three-year history for prediction generation.");
        Map<String, List<HistoricalDataEntity>> historyBySymbol = historicalDataService.getThreeYearHistoryBySymbol();
        log.info("[PredictionService] Loaded {} symbol histories from historical database", historyBySymbol == null ? 0 : historyBySymbol.size());

        List<PredictionEntity> predictions = new java.util.ArrayList<>();
        int skippedSymbols = 0;
        int validationSamples = 0;
        int correctValidationPredictions = 0;

        int totalSymbols = historyBySymbol == null ? 0 : historyBySymbol.size();
        int symbolProcessed = 0;

        for (Map.Entry<String, List<HistoricalDataEntity>> entry : historyBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<HistoricalDataEntity> symbolHistory = entry.getValue();
            symbolProcessed++;
            log.info("[PredictionService] Processing symbol {}/{}: symbol={} | historyRows={}",
                    symbolProcessed, totalSymbols, symbol,
                    symbolHistory == null ? 0 : symbolHistory.size());
            if (symbolHistory == null || symbolHistory.isEmpty()) {
                log.warn("[PredictionService] Skipping symbol={} because no history rows were loaded", symbol);
                continue;
            }
            try {
                symbolHistory.sort(Comparator.comparing(HistoricalDataEntity::getCreatedDate));
                int rowCount = symbolHistory.size();
                int validSampleCount = stockAiService.countValidPredictionSamples(symbolHistory);
                log.info("[PredictionService] Symbol={} rowCount={} validPredictionSamples={} | {}/{}",
                        symbol, rowCount, validSampleCount, symbolProcessed, totalSymbols);
                StockAIService.AnalysisResult analysis = stockAiService.analyze(symbolHistory);
                if (analysis == null) {
                    log.warn("[PredictionService] Analysis returned null for symbol={} rowCount={} validSampleCount={}; skipping symbol.",
                            symbol, rowCount, validSampleCount);
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
                entity.setCreatedDate(LocalDate.now());
                entity.setPredictionDate(LocalDate.now());
                entity.setPredictionClose(analysis.getPredictedClose());
                entity.setPredictionDirection(analysis.getDirection());
                entity.setPredictionClosePercentage(analysis.getPredictedClosePercentage());
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
                log.info("[PredictionService] Prediction created for symbol={} direction={} confidence={} | {}/{}",
                        symbol, analysis.getDirection(), analysis.getConfidencePercent(), symbolProcessed, totalSymbols);
            } catch (Exception e) {
                log.error("[PredictionService] Failed to generate prediction for symbol={} during sync", symbol, e);
                skippedSymbols++;
            }
        }

        log.info("[PredictionService] Prediction generation finished. Generated={} records, skippedSymbols={}, validationSamples={}, correctValidationPredictions={}",
                predictions.size(), skippedSymbols, validationSamples, correctValidationPredictions);
        double validationAccuracyPercent = validationSamples == 0
                ? 0.0
                : correctValidationPredictions * 100.0 / validationSamples;

        UUID runId = UUID.randomUUID();
        LocalDateTime runCreatedAt = LocalDateTime.now();
        int predictionsSaved = predictions.size();

        for (PredictionEntity prediction : predictions) {
            prediction.setRunId(runId);
            prediction.setRunStartDate(LocalDate.now());
            prediction.setRunEndDate(LocalDate.now());
            prediction.setRunCreatedAt(runCreatedAt);
            prediction.setPredictionsSaved(predictionsSaved);
            prediction.setSkippedSymbols(skippedSymbols);
            prediction.setValidationSamples(validationSamples);
            prediction.setCorrectValidationPredictions(correctValidationPredictions);
            prediction.setValidationAccuracyPercent(validationAccuracyPercent);
            prediction.setAccuracyMetric("Next available session direction; chronological 80/20 holdout");
        }
        log.info("[PredictionService] Prepared {} prediction rows for persistence with validation_accuracy={}%", predictions.size(), validationAccuracyPercent);
        if (!predictions.isEmpty()) {
            for (int i = 0; i < predictions.size(); i += 500) {
                int end = Math.min(i + 500, predictions.size());
                List<PredictionEntity> batch = predictions.subList(i, end);
                log.info("[PredictionService] Saving prediction batch {} with {} records", (i / 500) + 1, batch.size());
                try {
                    predictionRepository.savePrediction(batch);
                    totalUpserted += batch.size();

                    List<HistoricalPredictionEntity> historicalBatch = new java.util.ArrayList<>();
                    for (PredictionEntity prediction : batch) {
                        HistoricalPredictionEntity historicalEntity = new HistoricalPredictionEntity();
                        BeanUtils.copyProperties(prediction, historicalEntity);
                        historicalEntity.setId(null);
                        historicalBatch.add(historicalEntity);
                    }
                    log.info("[PredictionService] Saving {} historical prediction records for audit trail", historicalBatch.size());
                    predictionRepository.saveHistoricalPrediction(historicalBatch);
                } catch (Exception e) {
                    log.error("[PredictionService] Failed to save prediction batch starting at index {} for {} records", i, batch.size(), e);
                    throw e;
                }
            }
        }
        log.info("[PredictionService] Prediction synchronization completed. Total upserted prediction records={}", totalUpserted);
    }

    @Override
    public List<PredictionDto> fetchHistoricalPredictionData(HistoricalPredictionFiltersDto filters) {
        try {
            if (filters == null) {
                return List.of();
            }
            List<HistoricalPredictionEntity> predictionHistory = predictionRepository.findHistoricalPredictionDataByFilters(filters);
            List<PredictionDto> response = new ArrayList<>();
            for (HistoricalPredictionEntity entity : predictionHistory) {
                PredictionDto dto = new PredictionDto();
                BeanUtils.copyProperties(entity, dto);
                response.add(dto);
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch historical prediction data for filters {}", filters, e);
            return List.of();
        }
    }
}
