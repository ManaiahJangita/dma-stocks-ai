package com.stockai.service.impl;

import com.stockai.dto.MarketComparisonDto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.MarketComparisonEntity;
import com.stockai.entity.MarketEntity;
import com.stockai.entity.MarketFeedbackEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.MarketComparisonRepository;
import com.stockai.repository.MarketFeedbackRepository;
import com.stockai.repository.MarketRepository;
import com.stockai.service.interf.EmailDeliveryService;
import com.stockai.service.interf.MarketComparisonService;
import com.stockai.service.interf.MarketFeedbackService;
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
public class MarketComparisonServiceImpl implements MarketComparisonService {

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private HistoricalDataRepository historicalDataRepository;

    @Autowired
    private MarketComparisonRepository marketComparisonRepository;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Autowired
    private MarketsServiceImpl marketsServiceImpl;

    @Autowired
    private MarketFeedbackRepository marketFeedbackRepository;

    @Autowired
    private MarketFeedbackService marketFeedbackService;

    @Value("${market.alert.email:}")
    private String alertEmail;

    @Value("${market.alert.threshold-percent:90.0}")
    private double alertThresholdPercent;

    @Override
    @Transactional
    public void compareYesterdayMarketFormulaWithTodaysActuals(LocalDate marketDate) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("[MarketComparison] Started at {} with marketDate={}", startTime, marketDate);

        LocalDate effectiveMarketDate = marketDate;
        if (effectiveMarketDate == null) {
            LocalDate latestMarketDate = findLatestMarketDate();
            LocalDate latestActualDate = historicalDataRepository.findLatestCreatedDate();
            if (latestMarketDate == null || latestActualDate == null) {
                log.info("[MarketComparison] No market or actual data available for comparison yet");
                return;
            }
            if (latestActualDate.isBefore(latestMarketDate)
                    || latestActualDate.equals(latestMarketDate)) {
                log.info("[MarketComparison] Latest actual date {} is not after latest market date {}. Skipping comparison until next-session actual data exists.", latestActualDate, latestMarketDate);
                return;
            }
            effectiveMarketDate = latestMarketDate;
        }

        LocalDate actualDate = null;
        if (marketDate == null) {
            for (int offset = 1; offset <= 7; offset++) {
                LocalDate candidate = effectiveMarketDate.plusDays(offset);
                if (historicalDataRepository.findAll(candidate, candidate).stream().findAny().isPresent()) {
                    actualDate = candidate;
                    break;
                }
            }
        } else {
            for (int offset = 1; offset <= 7; offset++) {
                LocalDate candidate = marketDate.plusDays(offset);
                if (historicalDataRepository.findAll(candidate, candidate).stream().findAny().isPresent()) {
                    actualDate = candidate;
                    break;
                }
            }
        }

        if (actualDate == null) {
            log.info("[MarketComparison] No subsequent actual market data available for comparison");
            return;
        }

        final LocalDate comparisonMarketDate = effectiveMarketDate;
        log.info("[MarketComparison] Comparing marketDate={} with actualDate={}", comparisonMarketDate, actualDate);
        List<MarketEntity> marketRows = marketRepository.findAllMarketData();
        if (marketRows == null || marketRows.isEmpty()) {
            log.info("[MarketComparison] No market records found for date {}", comparisonMarketDate);
            return;
        }

        List<MarketEntity> marketRowsForDate = marketRows.stream()
                .filter(row -> row.getMarketDate() != null && row.getMarketDate().equals(comparisonMarketDate))
                .toList();
        if (marketRowsForDate.isEmpty()) {
            log.info("[MarketComparison] No market records found for marketDate {}", comparisonMarketDate);
            return;
        }

        Map<String, HistoricalDataEntity> actualBySymbol = new HashMap<>();
        List<HistoricalDataEntity> actualRows = historicalDataRepository.findAll(actualDate, actualDate);
        if (actualRows != null) {
            for (HistoricalDataEntity row : actualRows) {
                if (row != null && row.getSymbol() != null) {
                    actualBySymbol.put(row.getSymbol(), row);
                }
            }
        }
        log.info("[MarketComparison] Loaded {} market rows for date {} and indexed {} actual rows by symbol for {}",
                marketRowsForDate.size(), effectiveMarketDate, actualBySymbol.size(), actualDate);

        List<MarketComparisonEntity> comparisons = new ArrayList<>();
        UUID marketRunId = UUID.randomUUID();

        for (MarketEntity row : marketRowsForDate) {
            HistoricalDataEntity actual = actualBySymbol.get(row.getSymbol());
            if (actual == null) {
                log.debug("[MarketComparison] No actual record found for symbol {} on date {}", row.getSymbol(), actualDate);
                continue;
            }

            MarketComparisonEntity comparison = new MarketComparisonEntity();
            comparison.setSymbol(row.getSymbol());
            comparison.setMarketRunId(marketRunId);
            comparison.setMarketDate(effectiveMarketDate);
            comparison.setActualDate(actual.getCreatedDate());
            comparison.setPredictedOpen(row.getOpen());
            comparison.setPredictedHigh(row.getNextDayHigh() != null ? row.getNextDayHigh() : row.getHigh());
            comparison.setPredictedLow(row.getNextDayLow() != null ? row.getNextDayLow() : row.getLow());
            double predictedClose = row.getClose() == null ? 0.0
                    : row.getClose() * (1.0 + (row.getNextDayReturnPct() == null ? 0.0 : row.getNextDayReturnPct()) / 100.0);

            comparison.setPredictedClose(predictedClose);
            comparison.setPredictedDirection(determineDirection(row.getNextDayReturnPct()));
            comparison.setActualDirection(actual.getClose() >= row.getClose() ? "UP" : "DOWN");
            comparison.setActualOpen(actual.getOpen());
            comparison.setActualHigh(actual.getHigh());
            comparison.setActualLow(actual.getLow());
            comparison.setActualClose(actual.getClose());
            comparison.setCloseDiff(actual.getClose() - predictedClose);

            comparison.setCloseDiffPercent(predictedClose == 0.0
                    ? 0.0 : ((actual.getClose() - predictedClose) / predictedClose) * 100.0);

            comparison.setHighDiff(actual.getHigh() - comparison.getPredictedHigh());
            comparison.setLowDiff(actual.getLow() - comparison.getPredictedLow());
            comparison.setHighDiffPercent(comparison.getPredictedHigh() == null || comparison.getPredictedHigh() == 0.0
                    ? 0.0 : ((actual.getHigh() - comparison.getPredictedHigh()) / comparison.getPredictedHigh()) * 100.0);
            comparison.setLowDiffPercent(comparison.getPredictedLow() == null || comparison.getPredictedLow() == 0.0
                    ? 0.0 : ((actual.getLow() - comparison.getPredictedLow()) / comparison.getPredictedLow()) * 100.0);

            double upProbability = row.getNextDayReturnPct() == null
                    ? 0.50
                    : Math.max(0.0, Math.min(1.0, 0.50 + row.getNextDayReturnPct() / 100.0));
            comparison.setPredictedProbabilityUp(upProbability);
            comparison.setPredictedProbabilityDown(1.0 - upProbability);
            comparison.setConfidencePercent(row.getConfidenceScore());
            comparison.setPredictedCloseChangePct(row.getNextDayReturnPct());
            comparison.setPredictedHighChangePct(comparison.getPredictedHigh() == null || row.getClose() == null || row.getClose() == 0.0
                    ? 0.0 : ((comparison.getPredictedHigh() - row.getClose()) / row.getClose()) * 100.0);
            comparison.setPredictedLowChangePct(comparison.getPredictedLow() == null || row.getClose() == null || row.getClose() == 0.0
                    ? 0.0 : ((comparison.getPredictedLow() - row.getClose()) / row.getClose()) * 100.0);
            comparison.setModelValidationAccuracyPercent(row.getConfidenceScore());

            boolean directionMatched = comparison.getPredictedDirection() != null && comparison.getPredictedDirection().equalsIgnoreCase(comparison.getActualDirection());
            comparison.setDirectionMatched(directionMatched);
            comparison.setActualVsPredictedDirectionMatch(directionMatched);
            comparison.setStatus(directionMatched ? "MATCHED" : "MISSED");
            comparison.setRetrainingFeedbackScore(calculateRetrainingFeedbackScore(actual, row, comparison));
            comparison.setReason(buildReason(row, comparison));
            comparison.setCreatedAt(LocalDate.now());

            boolean targetReached = comparison.getPredictedHigh() != null
                    && comparison.getPredictedHigh() > 0.0
                    && actual.getHigh() >= comparison.getPredictedHigh();
            boolean sellRecommended = targetReached
                    && "UP".equalsIgnoreCase(comparison.getPredictedDirection());

            comparison.setTargetReached(targetReached);
            comparison.setSellRecommended(sellRecommended);
            comparison.setTargetStatus(sellRecommended ? "SELL" : (targetReached ? "TARGET_REACHED" : "TARGET_NOT_REACHED"));

            if (sellRecommended) {
                comparison.setReason(buildReason(row, comparison)
                        + " Predicted high was reached; consider SELL at or near the target.");
            } else {
                comparison.setReason(buildReason(row, comparison));
            }

            comparisons.add(comparison);

        }

        if (!comparisons.isEmpty()) {
            LocalDateTime beforePersist = LocalDateTime.now();
            log.info("[MarketComparison] Persisting {} comparison records for {} started at {}", comparisons.size(), effectiveMarketDate, beforePersist);
            marketComparisonRepository.archiveComparison(comparisons);
            marketComparisonRepository.deleteByMarketDate();
            marketComparisonRepository.saveComparison(comparisons);
            log.info("[MarketComparison] Saved {} comparison records to repository for market date {}", comparisons.size(), effectiveMarketDate);

            // Persist comparison results in the feedback queue first so the retraining workflow can
            // use the same database-backed dataset without breaking the existing comparison flow.
            marketFeedbackService.saveBulkFeedback(comparisons);
            applyFeedbackBasedRetraining(comparisons);

            evaluateAndSendBuyAlertIfThresholdReached(effectiveMarketDate);
            log.info("[MarketComparison] Completed persistence and follow-up checks for {} in {} ms", effectiveMarketDate,
                    Duration.between(beforePersist, LocalDateTime.now()).toMillis());
        } else {
            log.warn("[MarketComparison] No valid comparisons created for marketDate {}", effectiveMarketDate);
        }

        long elapsedMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        log.info("[MarketComparison] Finished at {}. Total runtime={} ms. ComparisonsCreated={}",
                LocalDateTime.now(), elapsedMs, comparisons.size());
    }

    private void applyFeedbackBasedRetraining(List<MarketComparisonEntity> comparisons) {
        if (comparisons == null || comparisons.isEmpty()) {
            return;
        }

        List<MarketFeedbackEntity> unprocessedFeedback = marketFeedbackService.getUnprocessedFeedback();
        if (unprocessedFeedback == null || unprocessedFeedback.isEmpty()) {
            log.info("[MarketComparison] No unprocessed market feedback found; skipping adaptive retraining.");
            return;
        }

        log.info("[MarketComparison] Applying adaptive market retraining using {} queued feedback records.", unprocessedFeedback.size());
        marketsServiceImpl.createNewVersionFromFeedback(comparisons);

        List<UUID> ids = unprocessedFeedback.stream()
                .map(MarketFeedbackEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!ids.isEmpty()) {
            marketFeedbackService.markProcessed(ids);
            marketFeedbackService.deleteProcessedEntriesOlderThan(LocalDateTime.now().minusDays(7));
        }
    }

    @Override
    public List<MarketComparisonDto> getComparisonsByMarketDate(LocalDate marketDate) {
        if (marketDate == null) {
            marketDate = LocalDate.now().minusDays(1);
        }
        return mapToDtos(marketComparisonRepository.findByMarketDate(marketDate));
    }

    @Override
    public List<MarketComparisonDto> getComparisonsBySymbolAndMarketDate(String symbol, LocalDate marketDate) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return getComparisonsByMarketDate(marketDate);
        }
        if (marketDate == null) {
            marketDate = LocalDate.now().minusDays(1);
        }
        return mapToDtos(marketComparisonRepository.findBySymbolAndMarketDate(symbol, marketDate));
    }

    @Override
    public List<MarketComparisonDto> getComparisonHistoryByMarketDate(LocalDate marketDate) {
        if (marketDate == null) {
            marketDate = LocalDate.now().minusDays(1);
        }
        return mapToDtos(marketComparisonRepository.findByMarketDate(marketDate));
    }

    @Override
    public List<MarketComparisonDto> getComparisonHistoryBySymbolAndMarketDate(String symbol, LocalDate marketDate) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return getComparisonHistoryByMarketDate(marketDate);
        }
        if (marketDate == null) {
            marketDate = LocalDate.now().minusDays(1);
        }
        return mapToDtos(marketComparisonRepository.findBySymbolAndMarketDate(symbol, marketDate));
    }

    @Override
    public void evaluateAndSendBuyAlertIfThresholdReached(LocalDate marketDate) {
        if (marketDate == null) {
            marketDate = LocalDate.now().minusDays(1);
        }

        List<MarketComparisonEntity> comparisons = marketComparisonRepository.findByMarketDate(marketDate);
        if (comparisons == null || comparisons.isEmpty()) {
            log.info("No comparison rows found for {} so no buy alert is triggered.", marketDate);
            return;
        }

        long matchedCount = comparisons.stream().filter(item -> Boolean.TRUE.equals(item.getDirectionMatched())).count();
        double accuracyPercent = (matchedCount * 100.0) / comparisons.size();

        log.info("Market comparison accuracy for {} = {}% (matched {} of {})", marketDate, accuracyPercent, matchedCount, comparisons.size());

        if (accuracyPercent >= alertThresholdPercent && alertEmail != null && !alertEmail.isBlank()) {
            String subject = "Market Buy Alert - Formula Accuracy Reached " + String.format("%.2f", accuracyPercent) + "%";
            String body = "Market formula reached the buy threshold on " + marketDate + ".\n\n"
                    + "Accuracy: " + String.format("%.2f", accuracyPercent) + "%\n"
                    + "Matched predictions: " + matchedCount + " / " + comparisons.size() + "\n\n"
                    + "This means the market formula is currently strong enough to consider buying stocks for the next trading day.\n\n"
                    + "Important: This is an alert, not financial advice.\n"
                    + "Please validate with your own risk management and market conditions before placing trades.";

            emailDeliveryService.sendPredictionAccuracyAlert(alertEmail, subject, body);
            log.info("Buy alert email sent to {} for market date {}", alertEmail, marketDate);
        }
    }

    private LocalDate findLatestMarketDate() {
        List<MarketEntity> rows = marketRepository.findAllMarketData();
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.stream()
                .map(MarketEntity::getMarketDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

    }

    private String determineDirection(Double returnPct) {
        if (returnPct == null || Math.abs(returnPct) < 0.0001) {
            return "NEUTRAL";
        }
        return returnPct > 0.0 ? "UP" : "DOWN";
    }

    private double calculateRetrainingFeedbackScore(HistoricalDataEntity actual, MarketEntity modelSignal, MarketComparisonEntity comparison) {
        double directionScore = comparison.getActualVsPredictedDirectionMatch() ? 1.0 : 0.0;
        double closeError = Math.abs(comparison.getCloseDiff() == null ? 0.0 : comparison.getCloseDiff());
        double highError = Math.abs(comparison.getHighDiff() == null ? 0.0 : comparison.getHighDiff());
        double lowError = Math.abs(comparison.getLowDiff() == null ? 0.0 : comparison.getLowDiff());
        double relativeCloseError = actual.getClose() == 0 ? 0.0 : (closeError / actual.getClose());
        double relativeHighError = actual.getHigh() == 0 ? 0.0 : (highError / actual.getHigh());
        double relativeLowError = actual.getLow() == 0 ? 0.0 : (lowError / actual.getLow());
        double priceErrorScore = Math.max(0.0, 1.0 - (relativeCloseError * 10.0 + relativeHighError * 5.0 + relativeLowError * 5.0));
        double confidencePenalty = modelSignal.getConfidenceScore() == null ? 0.0 : Math.max(0.0, (100.0 - modelSignal.getConfidenceScore()) / 100.0);
        double weightedScore = (directionScore * 0.5) + (Math.max(0.0, Math.min(1.0, priceErrorScore)) * 0.5);
        return Math.max(0.0, Math.min(100.0, (weightedScore * 100.0) - (confidencePenalty * 25.0)));
    }

    private String buildReason(MarketEntity modelSignal, MarketComparisonEntity comparison) {
        double closeError = Math.abs(comparison.getCloseDiff() == null ? 0.0 : comparison.getCloseDiff());
        double highError = Math.abs(comparison.getHighDiff() == null ? 0.0 : comparison.getHighDiff());
        double lowError = Math.abs(comparison.getLowDiff() == null ? 0.0 : comparison.getLowDiff());

        StringBuilder reason = new StringBuilder();
        if (comparison.getDirectionMatched() != null && comparison.getDirectionMatched()) {
            reason.append("Direction matched; ");
        } else {
            reason.append("Direction mismatched; ");
        }

        if (closeError <= 0.5) {
            reason.append("close was near prediction; ");
        } else if (closeError <= 2.0) {
            reason.append("close deviation was moderate; ");
        } else {
            reason.append("close deviation was high; ");
        }

        if (highError > lowError) {
            reason.append("high drifted more than low. ");
        } else if (lowError > highError) {
            reason.append("low drifted more than high. ");
        } else {
            reason.append("spread stayed within expected band. ");
        }

        if (modelSignal.getNextDayReturnPct() != null && modelSignal.getNextDayReturnPct() > 0 && comparison.getActualDirection() != null && comparison.getActualDirection().equalsIgnoreCase("DOWN")) {
            reason.append("Likely due to trend reversal or a sharp intraday move.");
        }

        return reason.toString();
    }

    private List<MarketComparisonDto> mapToDtos(List<MarketComparisonEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        List<MarketComparisonDto> dtos = new ArrayList<>();
        for (MarketComparisonEntity entity : entities) {
            MarketComparisonDto dto = new MarketComparisonDto();
            dto.setId(entity.getId());
            dto.setSymbol(entity.getSymbol());
            dto.setMarketRunId(entity.getMarketRunId());
            dto.setMarketDate(entity.getMarketDate());
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
            dto.setTargetReached(entity.getTargetReached());
            dto.setSellRecommended(entity.getSellRecommended());
            dto.setTargetStatus(entity.getTargetStatus());
            dtos.add(dto);
        }
        return dtos;
    }
}

