package com.stockai.service.impl;

import com.stockai.dto.PredictionComparisonDto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.PredictionComparisonEntity;
import com.stockai.entity.PredictionEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.PredictionComparisonRepository;
import com.stockai.repository.PredictionRepository;
import com.stockai.service.interf.EmailDeliveryService;
import com.stockai.service.interf.PredictionComparisonService;
import com.stockai.service.interf.RetrainingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class PredictionComparisonServiceImpl implements PredictionComparisonService {

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private HistoricalDataRepository historicalDataRepository;

    @Autowired
    private PredictionComparisonRepository predictionComparisonRepository;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Autowired
    private RetrainingService retrainingService;

    @Value("${prediction.alert.email:}")
    private String alertEmail;

    @Value("${prediction.alert.threshold-percent:90.0}")
    private double alertThresholdPercent;

    @Override
    @Transactional
    public void compareYesterdayPredictionsWithTodaysActuals(LocalDate predictionDate) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("[PredictionComparison] Started at {} with predictionDate={}", startTime, predictionDate);
        LocalDate effectivePredictionDate = predictionDate;
        if (effectivePredictionDate == null) {
            LocalDate latestPredictionDate = predictionRepository.findLatestPredictionDate();
            LocalDate latestActualDate = historicalDataRepository.findLatestCreatedDate();

            if (latestPredictionDate == null || latestActualDate == null) {
                log.info("[PredictionComparison] No prediction or actual data available for comparison yet");
                return;
            }

            if (latestActualDate.isBefore(latestPredictionDate)
                    || latestActualDate.equals(latestPredictionDate)) {
                log.info("[PredictionComparison] Latest actual date {} is not after latest prediction date {}. Skipping comparison until next-session actual data exists.", latestActualDate, latestPredictionDate);
                return;
            }

            effectivePredictionDate = latestPredictionDate;
            log.info("[PredictionComparison] Using latest prediction date {} and latest actual date {} for comparison", effectivePredictionDate, latestActualDate);
        }

        LocalDate actualDate = predictionDate == null
                ? historicalDataRepository.findLatestCreatedDate()
                : null;
        if (predictionDate != null) {
            for (int offset = 1; offset <= 7; offset++) {
                LocalDate candidate = predictionDate.plusDays(offset);
                if (historicalDataRepository.findAll(candidate, candidate).stream().findAny().isPresent()) {
                    actualDate = candidate;
                    break;
                }
            }
        }
        if (actualDate == null) {
            log.info("[PredictionComparison] No subsequent actual market data available for comparison");
            return;
        }

        if (predictionDate == null) {
            log.info("[PredictionComparison] Comparing latest prediction date {} with latest actual date {}", effectivePredictionDate, actualDate);
        } else {
            log.info("[PredictionComparison] Comparing prediction date {} with actual date {}", effectivePredictionDate, actualDate);
        }

        List<PredictionEntity> predictedRows = predictionRepository.findByCreatedDateBetween(effectivePredictionDate, effectivePredictionDate);
        if (predictedRows == null || predictedRows.isEmpty()) {
            log.info("[PredictionComparison] No prediction records found for date {}", effectivePredictionDate);
            return;
        }

        Map<String, HistoricalDataEntity> actualBySymbol = new HashMap<>();
        List<HistoricalDataEntity> actualRows = historicalDataRepository.findAll(actualDate, actualDate);
        log.info("[PredictionComparison] Loaded {} predicted rows and {} actual rows for comparison window", predictedRows.size(), actualRows == null ? 0 : actualRows.size());
        if (actualRows != null) {
            for (HistoricalDataEntity row : actualRows) {
                if (row != null && row.getSymbol() != null) {
                    actualBySymbol.put(row.getSymbol(), row);
                }
            }
        }
        log.info("[PredictionComparison] Actual data indexed by symbol. indexSize={}", actualBySymbol.size());

        List<PredictionComparisonEntity> comparisons = new ArrayList<>();

        for (PredictionEntity predicted : predictedRows) {
            HistoricalDataEntity actual = actualBySymbol.get(predicted.getSymbol());
            if (actual == null) {
                log.debug("[PredictionComparison] No actual record found for symbol {} on date {}", predicted.getSymbol(), actualDate);
                continue;
            }

            PredictionComparisonEntity comparison = new PredictionComparisonEntity();
            comparison.setSymbol(predicted.getSymbol());
            comparison.setPredictionRunId(predicted.getRunId());
            comparison.setPredictionDate(predicted.getPredictionDate());
            comparison.setActualDate(actual.getCreatedDate());
            comparison.setPredictedOpen(predicted.getOpen());
            comparison.setPredictedHigh(predicted.getPredictedHigh());
            comparison.setPredictedLow(predicted.getPredictedLow());
            comparison.setPredictedClose(predicted.getPredictionClose());
            comparison.setPredictedDirection(predicted.getPredictionDirection());
            comparison.setActualOpen(actual.getOpen());
            comparison.setActualHigh(actual.getHigh());
            comparison.setActualLow(actual.getLow());
            comparison.setActualClose(actual.getClose());
            comparison.setActualDirection(actual.getClose() >= predicted.getClose() ? "UP" : "DOWN");

            comparison.setPredictedProbabilityUp(predicted.getUpProbabilityPercent() == null ? 0.0 : predicted.getUpProbabilityPercent() / 100.0);
            comparison.setPredictedProbabilityDown(predicted.getDownProbabilityPercent() == null ? 0.0 : predicted.getDownProbabilityPercent() / 100.0);
            comparison.setConfidencePercent(predicted.getConfidencePercent());
            comparison.setPredictedHighChangePct(predicted.getPredictedHighPercentage());
            comparison.setPredictedLowChangePct(predicted.getPredictedLowPercentage());
            comparison.setPredictedCloseChangePct(predicted.getPredictionClosePercentage());
            comparison.setModelValidationAccuracyPercent(predicted.getValidationAccuracyPercent());

            comparison.setCloseDiff(actual.getClose() - predicted.getPredictionClose());
            comparison.setHighDiff(actual.getHigh() - predicted.getPredictedHigh());
            comparison.setLowDiff(actual.getLow() - predicted.getPredictedLow());

            comparison.setCloseDiffPercent((predicted.getPredictionClose() == null || predicted.getPredictionClose() == 0.0
                    ? 0.0 : ((actual.getClose() - predicted.getPredictionClose()) / predicted.getPredictionClose()) * 100.0));
            comparison.setHighDiffPercent((predicted.getPredictedHigh() == null || predicted.getPredictedHigh() == 0.0
                    ? 0.0 : ((actual.getHigh() - predicted.getPredictedHigh()) / predicted.getPredictedHigh()) * 100.0));
            comparison.setLowDiffPercent((predicted.getPredictedLow() == null || predicted.getPredictedLow() == 0.0
                    ? 0.0 : ((actual.getLow() - predicted.getPredictedLow()) / predicted.getPredictedLow()) * 100.0));

            boolean directionMatched = predicted.getPredictionDirection() != null
                    && predicted.getPredictionDirection().equalsIgnoreCase(comparison.getActualDirection());

            comparison.setDirectionMatched(directionMatched);
            comparison.setActualVsPredictedDirectionMatch(directionMatched);
            comparison.setStatus(directionMatched ? "MATCHED" : "MISSED");
            comparison.setRetrainingFeedbackScore(calculateRetrainingFeedbackScore(predicted, actual, comparison));
            comparison.setReason(buildReason(predicted, comparison));
            comparison.setCreatedAt(LocalDate.now());

            comparisons.add(comparison);
            log.info("[PredictionComparison] Symbol={} predictedDirection={} actualDirection={} closeDiff={} match={} confidence={} | actualClose={} predictedClose={}",
                    predicted.getSymbol(), predicted.getPredictionDirection(), comparison.getActualDirection(), comparison.getCloseDiff(), directionMatched,
                    predicted.getConfidencePercent(), actual.getClose(), predicted.getPredictionClose());
        }

        if (!comparisons.isEmpty()) {
            LocalDateTime beforePersist = LocalDateTime.now();
            log.info("[PredictionComparison] Persisting {} comparison records for {} started at {}", comparisons.size(), effectivePredictionDate, beforePersist);
            predictionComparisonRepository.archiveComparison(comparisons);
            predictionComparisonRepository.truncatePredictionComparisonData();
            predictionComparisonRepository.saveComparison(comparisons);
            log.info("[PredictionComparison] Saved {} comparison records to repository for prediction date {}", comparisons.size(), effectivePredictionDate);
            evaluateAndSendBuyAlertIfThresholdReached(effectivePredictionDate);
            retrainingService.checkAndTriggerModelRetraining(effectivePredictionDate);
            log.info("[PredictionComparison] Completed persistence and follow-up checks for {} in {} ms", effectivePredictionDate,
                    Duration.between(beforePersist, LocalDateTime.now()).toMillis());
        } else {
            log.warn("[PredictionComparison] No valid comparisons created for predictionDate {}", effectivePredictionDate);
        }

        long elapsedMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        log.info("[PredictionComparison] Finished at {}. Total runtime={} ms. ComparisonsCreated={}",
                LocalDateTime.now(), elapsedMs, comparisons.size());
    }

    @Override
    public List<PredictionComparisonDto> getComparisonsByPredictionDate(LocalDate predictionDate) {
        if (predictionDate == null) {
            predictionDate = LocalDate.now().minusDays(1);
        }
        List<PredictionComparisonEntity> entities = predictionComparisonRepository.findByPredictionDate(predictionDate);
        return mapToDtos(entities);
    }

    @Override
    public List<PredictionComparisonDto> getComparisonsBySymbolAndPredictionDate(String symbol, LocalDate predictionDate) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return getComparisonsByPredictionDate(predictionDate);
        }
        if (predictionDate == null) {
            predictionDate = LocalDate.now().minusDays(1);
        }
        List<PredictionComparisonEntity> entities = predictionComparisonRepository.findBySymbolAndPredictionDate(symbol, predictionDate);
        return mapToDtos(entities);
    }

    @Override
    public List<PredictionComparisonDto> getComparisonHistoryByPredictionDate(LocalDate predictionDate) {
        if (predictionDate == null) {
            predictionDate = LocalDate.now().minusDays(1);
        }
        List<PredictionComparisonEntity> entities = predictionComparisonRepository.findByPredictionDate(predictionDate);
        return mapToDtos(entities);
    }

    @Override
    public List<PredictionComparisonDto> getComparisonHistoryBySymbolAndPredictionDate(String symbol, LocalDate predictionDate) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return getComparisonHistoryByPredictionDate(predictionDate);
        }
        if (predictionDate == null) {
            predictionDate = LocalDate.now().minusDays(1);
        }
        List<PredictionComparisonEntity> entities = predictionComparisonRepository.findBySymbolAndPredictionDate(symbol, predictionDate);
        return mapToDtos(entities);
    }

    @Override
    public void evaluateAndSendBuyAlertIfThresholdReached(LocalDate predictionDate) {
        if (predictionDate == null) {
            predictionDate = LocalDate.now().minusDays(1);
        }

        List<PredictionComparisonEntity> comparisons = predictionComparisonRepository.findByPredictionDate(predictionDate);
        if (comparisons == null || comparisons.isEmpty()) {
            log.info("No comparison rows found for {} so no buy alert is triggered.", predictionDate);
            return;
        }

        long matchedCount = comparisons.stream()
                .filter(item -> Boolean.TRUE.equals(item.getDirectionMatched()))
                .count();

        double accuracyPercent = (matchedCount * 100.0) / comparisons.size();

        log.info("Prediction comparison accuracy for {} = {}% (matched {} of {})", predictionDate, accuracyPercent, matchedCount, comparisons.size());

        if (accuracyPercent >= alertThresholdPercent && alertEmail != null && !alertEmail.isBlank()) {
            String subject = "Stock Buy Alert - Prediction Accuracy Reached " + String.format("%.2f", accuracyPercent) + "%";
            String body = "Prediction model reached the buy threshold on " + predictionDate + ".\n\n"
                    + "Accuracy: " + String.format("%.2f", accuracyPercent) + "%\n"
                    + "Matched predictions: " + matchedCount + " / " + comparisons.size() + "\n\n"
                    + "This means the model is currently strong enough to consider buying stocks for the next trading day based on the same model logic.\n\n"
                    + "Important: This is an alert, not financial advice.\n"
                    + "Please validate with your own risk management and market conditions before placing trades.";

            emailDeliveryService.sendPredictionAccuracyAlert(alertEmail, subject, body);
            log.info("Buy alert email sent to {} for prediction date {}", alertEmail, predictionDate);
        }
    }

    private List<PredictionComparisonDto> mapToDtos(List<PredictionComparisonEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        List<PredictionComparisonDto> dtos = new ArrayList<>();
        for (PredictionComparisonEntity entity : entities) {
            PredictionComparisonDto dto = new PredictionComparisonDto();
            dto.setId(entity.getId());
            dto.setSymbol(entity.getSymbol());
            dto.setPredictionRunId(entity.getPredictionRunId());
            dto.setPredictionDate(entity.getPredictionDate());
            dto.setActualDate(entity.getActualDate());
            dto.setPredictedOpen(entity.getPredictedOpen());
            dto.setPredictedHigh(entity.getPredictedHigh());
            dto.setPredictedLow(entity.getPredictedLow());
            dto.setPredictedClose(entity.getPredictedClose());
            dto.setPredictedDirection(entity.getPredictedDirection());
            dto.setActualOpen(entity.getActualOpen());
            dto.setActualHigh(entity.getActualHigh());
            dto.setActualLow(entity.getActualLow());
            dto.setActualClose(entity.getActualClose());
            dto.setActualDirection(entity.getActualDirection());
            dto.setCloseDiff(entity.getCloseDiff());
            dto.setHighDiff(entity.getHighDiff());
            dto.setLowDiff(entity.getLowDiff());
            dto.setCloseDiffPercent(entity.getCloseDiffPercent());
            dto.setHighDiffPercent(entity.getHighDiffPercent());
            dto.setLowDiffPercent(entity.getLowDiffPercent());
            dto.setDirectionMatched(entity.getDirectionMatched());
            dto.setActualVsPredictedDirectionMatch(entity.getActualVsPredictedDirectionMatch());
            dto.setStatus(entity.getStatus());
            dto.setReason(entity.getReason());
            dto.setCreatedAt(entity.getCreatedAt());
            dto.setPredictedProbabilityUp(entity.getPredictedProbabilityUp());
            dto.setPredictedProbabilityDown(entity.getPredictedProbabilityDown());
            dto.setConfidencePercent(entity.getConfidencePercent());
            dto.setPredictedHighChangePct(entity.getPredictedHighChangePct());
            dto.setPredictedLowChangePct(entity.getPredictedLowChangePct());
            dto.setPredictedCloseChangePct(entity.getPredictedCloseChangePct());
            dto.setModelValidationAccuracyPercent(entity.getModelValidationAccuracyPercent());
            dto.setRetrainingFeedbackScore(entity.getRetrainingFeedbackScore());
            dtos.add(dto);
        }
        return dtos;
    }

    private double calculateRetrainingFeedbackScore(PredictionEntity predicted, HistoricalDataEntity actual, PredictionComparisonEntity comparison) {
        double directionScore = comparison.getActualVsPredictedDirectionMatch() ? 1.0 : 0.0;
        double closeError = Math.abs(comparison.getCloseDiff() == null ? 0.0 : comparison.getCloseDiff());
        double highError = Math.abs(comparison.getHighDiff() == null ? 0.0 : comparison.getHighDiff());
        double lowError = Math.abs(comparison.getLowDiff() == null ? 0.0 : comparison.getLowDiff());
        double relativeCloseError = actual.getClose() <= 0 ? 0.0 : (closeError / actual.getClose());
        double relativeHighError = actual.getHigh() <= 0 ? 0.0 : (highError / actual.getHigh());
        double relativeLowError = actual.getLow() <= 0 ? 0.0 : (lowError / actual.getLow());
        double priceErrorScore = Math.max(0.0, 1.0 - (relativeCloseError * 10.0 + relativeHighError * 5.0 + relativeLowError * 5.0));
        double confidencePenalty = predicted.getConfidencePercent() == null ? 0.0 : Math.max(0.0, (100.0 - predicted.getConfidencePercent()) / 100.0);
        double correctedPriceErrorScore = Math.clamp(priceErrorScore, 0.0, 1.0);
        double weightedScore = (directionScore * 0.5) + (correctedPriceErrorScore * 0.5);
        return Math.clamp((weightedScore * 100.0) - (confidencePenalty * 25.0), 0.0, 100.0);
    }

    private String buildReason(PredictionEntity predicted, PredictionComparisonEntity comparison) {
        double closeError = Math.abs(comparison.getCloseDiff() == null ? 0.0 : comparison.getCloseDiff());
        double highError = Math.abs(comparison.getHighDiff() == null ? 0.0 : comparison.getHighDiff());
        double lowError = Math.abs(comparison.getLowDiff() == null ? 0.0 : comparison.getLowDiff());

        StringBuilder reason = new StringBuilder();

        if (comparison.getDirectionMatched() != null && comparison.getDirectionMatched()) {
            reason.append("Direction matched; ");
        } else {
            reason.append("Direction mismatched; ");
        }

        if (closeError <= 1.0) {
            reason.append("close was within 1 point; ");
        } else if (closeError <= 3.0) {
            reason.append("close deviation was moderate; ");
        } else {
            reason.append("close deviation was high; ");
        }

        if (highError > lowError) {
            reason.append("high prediction drifted more than low prediction. ");
        } else if (lowError > highError) {
            reason.append("low prediction drifted more than high prediction. ");
        } else {
            reason.append("high/low spread stayed near expected range. ");
        }

        if (predicted.getPredictionDirection() != null && !predicted.getPredictionDirection().equalsIgnoreCase(comparison.getActualDirection())) {
            reason.append("Likely due to sudden market reversal, volatility spike, or gap opening.");
        }

        return reason.toString();
    }
}
