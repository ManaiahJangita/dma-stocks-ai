package com.stockai.service.impl;

import com.stockai.dto.MarketDto;
import com.stockai.dto.MarketFiltersDto;
import com.stockai.dto.MarketHistoryFiltersDto;
import com.stockai.entity.*;
import com.stockai.repository.*;
import com.stockai.service.interf.MarketsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MarketsServiceImpl implements MarketsService {
    private static final Map<String, Double> MARKET_SIGNAL_WEIGHTS = new HashMap<>();

    private static final double MIN_WEIGHT = 0.04;
    private static final double MAX_WEIGHT = 0.60;

    static {
        MARKET_SIGNAL_WEIGHTS.put("prevChange", 0.35);
        MARKET_SIGNAL_WEIGHTS.put("momentum", 0.25);
        MARKET_SIGNAL_WEIGHTS.put("trendBias", 0.18);
        MARKET_SIGNAL_WEIGHTS.put("rsiBias", 0.12);
        MARKET_SIGNAL_WEIGHTS.put("breakoutBias", 0.06);
        MARKET_SIGNAL_WEIGHTS.put("volumeBias", 0.04);
    }

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private HistoricalDataRepository historicalDataRepository;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private InstrumentsRepository instrumentsRepository;

    @Autowired
    private MarketModelVersionRepository marketModelVersionRepository;

    @Autowired
    private MarketModelWeightRepository marketModelWeightRepository;

    private static final String MARKET_MODEL_NAME = "market_signal_model";
    private static final String ACTIVE_VERSION_STATUS = "ACTIVE";
    private static final String ARCHIVED_VERSION_STATUS = "ARCHIVED";

    @Override
    public List<MarketDto> fetchMarketsData(MarketFiltersDto filters) {
        List<MarketEntity> marketEntities = marketRepository.findMarketsByFilters(filters);
        return marketEntities.stream().map(entity -> {
            MarketDto dto = new MarketDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).toList();
    }

    @Override
    public List<MarketDto> fetchMarketsData(Integer offset, Integer limit) {
        List<MarketEntity> marketEntities = marketRepository.findAllMarketData(offset, limit);
        return marketEntities.stream().map(entity -> {
            MarketDto dto = new MarketDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).toList();
    }

    @Override
    @Transactional
    public synchronized int syncTwoYearHistoricalData() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(3);

        try {
            log.info("Starting historical data sync from {} to {}", startDate, endDate);
            marketRepository.deleteHistoricalMarkets();
            log.info("Deleted existing historical markets data");

            Map<String, List<HistoricalDataEntity>> historyBySymbol = historicalDataService.getThreeYearHistoryBySymbol();
            log.info("Fetched {} symbol histories for the date range {} to {}", historyBySymbol.size(), startDate, endDate);

            List<HistoricalDataEntity> history = new ArrayList<>();
            historyBySymbol.values().forEach(history::addAll);
            log.info("Flattened {} historical data records for market sync", history.size());

            if (history == null || history.isEmpty()) {
                log.warn("No historical data found for the date range {} to {}", startDate, endDate);
                return 0;
            }

            List<MarketEntity> marketRows = new ArrayList<>(history.size());
            for (HistoricalDataEntity historical : history) {
                if (historical == null) {
                    log.warn("Skipping null historical data entity");
                    continue;
                }
                try {
                    MarketEntity market = new MarketEntity();
                    market.setName(historical.getSymbol());
                    market.setSymbol(historical.getSymbol());
                    market.setStatus("Historical");
                    market.setOpen(historical.getOpen());
                    market.setHigh(historical.getHigh());
                    market.setLow(historical.getLow());
                    market.setClose(historical.getClose());
                    market.setVolume(historical.getVolume());
                    market.setPer(historical.getPer());
                    market.setPtsC(historical.getPtsC());
                    market.setCreatedDate(LocalDate.now());
                    market.setUpdatedDate(LocalDate.now());
                    market.setHistoricalCreatedDate(historical.getCreatedDate().atStartOfDay());
                    market.setHistoricalUpdateDate(historical.getCreatedDate());
                    market.setMarketDate(historical.getCreatedDate());
                    marketRows.add(market);
                } catch (Exception e) {
                    log.error("Failed to convert historical row for symbol {} on date {}", historical.getSymbol(), historical.getCreatedDate(), e);
                }
            }

            log.info("Transformed {} historical entities to market entities", marketRows.size());

            if (!marketRows.isEmpty()) {
                log.info("Enriching market analytics for {} rows", marketRows.size());
                enrichMarketAnalytics(marketRows);
                log.info("Completed enriching market analytics");

                int totalUpserted = 0;
                List<HistoricalMarketEntity> historicalBatch = new ArrayList<>();
                for (int i = 0; i < marketRows.size(); i += 500) {
                    int end = Math.min(i + 500, marketRows.size());
                    List<MarketEntity> batch = marketRows.subList(i, end);
                    log.info("Upserting batch {} - rows {} to {} (batch size: {})",
                            (i / 500 + 1), i, end, batch.size());

                    try {
                        marketRepository.upsertHistoricalMarkets(batch);
                        log.info("Successfully upserted {} market rows to market table", batch.size());
                        totalUpserted += batch.size();
                    } catch (Exception e) {
                        log.error("Error upserting market batch at offset {}", i, e);
                        throw e;
                    }
                    for (MarketEntity market : batch) {
                        HistoricalMarketEntity historicalMarket = new HistoricalMarketEntity();
                        BeanUtils.copyProperties(market, historicalMarket);
                        historicalBatch.add(historicalMarket);
                    }
                }
                try {
                    marketRepository.upsertHistoricalMarketsEveryday(historicalBatch);
                    log.info("Successfully upserted {} rows to historical_market_everyday table", historicalBatch.size());
                } catch (Exception e) {
                    log.error("Error upserting historical market batch at offset ", e);
                    throw e;
                }
                log.info("Completed sync: Total {} market rows upserted in {} batches",
                        totalUpserted, (marketRows.size() + 499) / 500);
                return totalUpserted;
            }
            log.warn("No market rows were created after conversion for date range {} to {}", startDate, endDate);
            return 0;
        } catch (Exception e) {
            log.error("Fatal error during historical data sync", e);
            throw e;
        }
    }

    private void enrichMarketAnalytics(List<MarketEntity> marketRows) {
        Map<String, Double> activeWeights = loadActiveWeightsFromDb();

        // Group all records by symbol
        Map<String, List<MarketEntity>> bySymbol = new HashMap<>();
        for (MarketEntity row : marketRows) {
            if (row.getSymbol() == null || row.getClose() == null || row.getMarketDate() == null) {
                continue;
            }
            bySymbol.computeIfAbsent(row.getSymbol(), key -> new ArrayList<>()).add(row);
        }

        // For each symbol, keep only the latest record with analytics calculated from entire 2-year history
        List<MarketEntity> consolidatedRows = new ArrayList<>();

        for (List<MarketEntity> series : bySymbol.values()) {
            if (series.isEmpty()) {
                continue;
            }

            // Sort by market date (oldest to newest)
            series.sort(java.util.Comparator.comparing(MarketEntity::getMarketDate));

            // Use the most recent (last) record and calculate analytics based on entire 2-year history
            MarketEntity latest = series.get(series.size() - 1);
            MarketEntity previous = series.size() > 1 ? series.get(series.size() - 2) : null;

            // Calculate change percent from previous day
            if (previous != null && previous.getClose() != null && previous.getClose() > 0) {
                latest.setPreviousClose(previous.getClose());
                latest.setChangePercent(((latest.getClose() - previous.getClose()) / previous.getClose()) * 100);
            } else {
                latest.setPreviousClose(null);
                latest.setChangePercent(null);
            }

            // Calculate technical indicators using entire 2-year history
            latest.setSma5(MarketAnalytics.smaAt(series, series.size(), 5));
            latest.setSma10(MarketAnalytics.smaAt(series, series.size(), 10));
            latest.setSma20(MarketAnalytics.smaAt(series, series.size(), 20));
            latest.setSma50(MarketAnalytics.smaAt(series, series.size(), 50));
            latest.setEma12(MarketAnalytics.smaAt(series, series.size(), 12));
            latest.setEma26(MarketAnalytics.smaAt(series, series.size(), 26));
            latest.setRsi14(MarketAnalytics.rsi(series, 14));
            latest.setBreakoutFlag(MarketAnalytics.breakout(series, 20));
            latest.setAvgVolume((long) MarketAnalytics.averageVolume(series, 20));

            // Calculate returns over various periods using entire history
            latest.setReturn1d(MarketAnalytics.changeOver(series, 1));
            latest.setReturn5d(MarketAnalytics.changeOver(series, 5));
            latest.setReturn10d(MarketAnalytics.changeOver(series, 10));
            latest.setReturn30d(MarketAnalytics.changeOver(series, 30));

            // Calculate 52-week high/low from 2-year history
            double high52 = series.stream()
                    .mapToDouble(m -> m.getHigh() == null ? m.getClose() : m.getHigh())
                    .max()
                    .orElse(latest.getClose());
            double low52 = series.stream()
                    .mapToDouble(m -> m.getLow() == null ? m.getClose() : m.getLow())
                    .min()
                    .orElse(latest.getClose());

            latest.setDistanceFrom52wHighPct(high52 > 0 ? ((high52 - latest.getClose()) / high52) * 100 : null);
            latest.setDistanceFrom52wLowPct(low52 > 0 ? ((latest.getClose() - low52) / low52) * 100 : null);
            latest.setDistanceFromSma20Pct(latest.getSma20() != null && latest.getSma20() > 0
                    ? ((latest.getSma20() - latest.getClose()) / latest.getSma20()) * 100 : null);

            // Calculate volume ratio
            if (latest.getVolume() != null && latest.getAvgVolume() != null && latest.getAvgVolume() > 0) {
                latest.setVolumeRatio(latest.getVolume().doubleValue() / latest.getAvgVolume());
            } else {
                latest.setVolumeRatio(null);
            }

            // Determine trend label
            String trendLabel = MarketAnalytics.trend(new MarketAnalytics.Snapshot(
                    latest.getSymbol(),
                    latest,
                    latest.getClose(),
                    latest.getChangePercent(),
                    latest.getVolume() == null ? 0 : latest.getVolume(),
                    latest.getRsi14(),
                    latest.getSma20(),
                    latest.getSma50(),
                    high52,
                    low52,
                    Boolean.TRUE.equals(latest.getBreakoutFlag())
            ));
            latest.setTrendLabel(trendLabel == null ? "Neutral" : trendLabel);

            // Calculate next day predictions using a weighted trend model instead of a raw previous-day move.
            double close = latest.getClose() == null ? 0.0 : latest.getClose();
            double prevChange = previous != null && previous.getClose() != null && previous.getClose() > 0
                    ? ((latest.getClose() - previous.getClose()) / previous.getClose()) * 100.0
                    : 0.0;
            double trendBias = latest.getSma20() != null && latest.getSma20() > 0
                    ? ((close - latest.getSma20()) / latest.getSma20()) * 100.0
                    : 0.0;
            double rsiBias = latest.getRsi14() != null ? (latest.getRsi14() - 50.0) * 0.8 : 0.0;
            double breakoutBias = Boolean.TRUE.equals(latest.getBreakoutFlag()) ? 1.5 : 0.0;
            double volumeBias = latest.getVolumeRatio() != null ? Math.min(8.0, latest.getVolumeRatio() * 5.0) : 0.0;
            double momentumBias = (latest.getReturn5d() != null ? latest.getReturn5d() : 0.0) * 0.35
                    + (latest.getReturn10d() != null ? latest.getReturn10d() : 0.0) * 0.20
                    + (latest.getReturn30d() != null ? latest.getReturn30d() : 0.0) * 0.10;

            double weightedMove = (prevChange * activeWeights.getOrDefault("prevChange", 0.35))
                    + (momentumBias * activeWeights.getOrDefault("momentum", 0.25))
                    + (trendBias * activeWeights.getOrDefault("trendBias", 0.18))
                    + (rsiBias * activeWeights.getOrDefault("rsiBias", 0.12))
                    + (breakoutBias * activeWeights.getOrDefault("breakoutBias", 0.06))
                    + (volumeBias * activeWeights.getOrDefault("volumeBias", 0.04));

            double nextMove = Math.max(-12.0, Math.min(12.0, weightedMove));
            double rangeFactor = Math.max(0.35, Math.abs(nextMove) * 0.70);
            double nextDayHigh = close * (1.0 + (rangeFactor + Math.max(0.0, nextMove)) / 100.0);
            double nextDayLow = close * (1.0 + (-rangeFactor + Math.min(0.0, nextMove)) / 100.0);
            double nextDayLowReturnPct = ((nextDayLow - close) / close) * 100.0;

            latest.setNextDayHigh(nextDayHigh);
            latest.setNextDayLow(nextDayLow);
            latest.setNextDayReturnPct(nextMove);
            latest.setNextDayLowReturnPct(nextDayLowReturnPct);

            double confidence = 50.0;
            if (latest.getRsi14() != null) {
                confidence += (latest.getRsi14() - 50.0) * 0.9;
            }
            if (Boolean.TRUE.equals(latest.getBreakoutFlag())) {
                confidence += 12.0;
            }
            if (latest.getVolumeRatio() != null) {
                confidence += Math.min(18.0, latest.getVolumeRatio() * 8.5);
            }
            if (latest.getSma20() != null && close > 0 && close > latest.getSma20()) {
                confidence += 8.0;
            } else if (latest.getSma20() != null && close > 0) {
                confidence -= 8.0;
            }
            if (latest.getReturn5d() != null) {
                confidence += Math.min(10.0, Math.abs(latest.getReturn5d()) * 0.4) * (latest.getReturn5d() >= 0 ? 1 : -1);
            }
            confidence = Math.max(0.0, Math.min(100.0, confidence));
            latest.setConfidenceScore(confidence);

            // Add only the latest record (one per symbol)
            consolidatedRows.add(latest);
        }

        // Replace the original list with consolidated records (one per symbol)
        marketRows.clear();
        marketRows.addAll(consolidatedRows);
        log.info("Consolidated market data from {} total records to {} unique symbols",
                bySymbol.values().stream().mapToInt(List::size).sum(), consolidatedRows.size());
    }

    @Override
    public List<MarketDto> fetchHistoricalMarketData(MarketHistoryFiltersDto filters) {
        try {
            if (filters == null) {
                return List.of();
            }
            List<HistoricalMarketEntity> marketHistoricalData = marketRepository.findHistoricalMarketDataByFilters(filters);
            List<MarketDto> response = new ArrayList<>();
            for (HistoricalMarketEntity entity : marketHistoricalData) {
                MarketDto dto = new MarketDto();
                BeanUtils.copyProperties(entity, dto);
                response.add(dto);
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch historical market data for filters {}", filters, e);
            return List.of();
        }
    }

    public static synchronized Map<String, Double> getCurrentMarketSignalWeights() {
        return new HashMap<>(MARKET_SIGNAL_WEIGHTS);
    }

    public synchronized Map<String, Double> loadActiveWeightsFromDb() {
        MarketModelVersionEntity activeVersion = marketModelVersionRepository
                .findTopByModelNameAndStatusOrderByCreatedAtDesc(MARKET_MODEL_NAME, ACTIVE_VERSION_STATUS)
                .orElse(null);

        if (activeVersion == null) {
            return getCurrentMarketSignalWeights();
        }

        List<MarketModelWeightEntity> weights = marketModelWeightRepository.findByModelVersionIdOrderBySignalName(activeVersion.getId());
        Map<String, Double> loaded = new HashMap<>();
        for (MarketModelWeightEntity weight : weights) {
            if (weight.getSignalName() != null) {
                loaded.put(weight.getSignalName(), weight.getWeightValue());
            }
        }

        if (loaded.size() == MARKET_SIGNAL_WEIGHTS.size()) {
            MARKET_SIGNAL_WEIGHTS.clear();
            MARKET_SIGNAL_WEIGHTS.putAll(loaded);
        }
        return new HashMap<>(MARKET_SIGNAL_WEIGHTS);
    }

    public synchronized void ensureDefaultActiveModelVersion() {
        MarketModelVersionEntity activeVersion = marketModelVersionRepository
                .findTopByModelNameAndStatusOrderByCreatedAtDesc(MARKET_MODEL_NAME, ACTIVE_VERSION_STATUS)
                .orElse(null);

        if (activeVersion != null) {
            return;
        }

        MarketModelVersionEntity version = new MarketModelVersionEntity();
        version.setModelName(MARKET_MODEL_NAME);
        version.setVersionLabel("v1.0.0");
        version.setStatus(ACTIVE_VERSION_STATUS);
        version.setCreatedAt(java.time.LocalDateTime.now());
        version.setTotalComparisons(0);
        version.setAverageAccuracyPercent(0.0);
        version.setNotes("Initial market signal model weights");
        version = marketModelVersionRepository.save(version);

        for (Map.Entry<String, Double> entry : MARKET_SIGNAL_WEIGHTS.entrySet()) {
            MarketModelWeightEntity weightEntity = new MarketModelWeightEntity();
            weightEntity.setModelVersionId(version.getId());
            weightEntity.setSignalName(entry.getKey());
            weightEntity.setWeightValue(entry.getValue());
            weightEntity.setCreatedAt(java.time.LocalDateTime.now());
            marketModelWeightRepository.save(weightEntity);
        }
    }

    public synchronized void createNewVersionFromFeedback(List<MarketComparisonEntity> comparisons) {
        ensureDefaultActiveModelVersion();

        MarketModelVersionEntity activeVersion = marketModelVersionRepository
                .findTopByModelNameAndStatusOrderByCreatedAtDesc(MARKET_MODEL_NAME, ACTIVE_VERSION_STATUS)
                .orElse(null);
        if (activeVersion == null) {
            return;
        }

        double averageScore = 0.0;
        for (MarketComparisonEntity comparison : comparisons) {
            double directionScore = Boolean.TRUE.equals(comparison.getDirectionMatched()) ? 1.0 : -0.35;
            double errorPercent = comparison.getCloseDiffPercent() == null ? 0.0 : Math.abs(comparison.getCloseDiffPercent());
            double errorScore = Math.max(0.0, 1.0 - (errorPercent / 10.0));
            averageScore += (directionScore * 0.7) + (errorScore * 0.3);
        }
        averageScore /= comparisons.size();

        Map<String, Double> adjustment = new HashMap<>();
        adjustment.put("prevChange", averageScore >= 0.55 ? 0.012 : -0.010);
        adjustment.put("momentum", averageScore >= 0.55 ? 0.012 : -0.010);
        adjustment.put("trendBias", averageScore >= 0.55 ? 0.010 : -0.008);
        adjustment.put("rsiBias", averageScore >= 0.55 ? 0.008 : -0.006);
        adjustment.put("breakoutBias", averageScore >= 0.55 ? 0.006 : -0.005);
        adjustment.put("volumeBias", averageScore >= 0.55 ? 0.004 : -0.004);

        for (Map.Entry<String, Double> entry : adjustment.entrySet()) {
            String key = entry.getKey();
            double current = MARKET_SIGNAL_WEIGHTS.getOrDefault(key, 0.10);
            double updated = current + entry.getValue();
            MARKET_SIGNAL_WEIGHTS.put(key, clamp(updated, MIN_WEIGHT, MAX_WEIGHT));
        }

        MarketModelVersionEntity newVersion = new MarketModelVersionEntity();
        newVersion.setModelName(MARKET_MODEL_NAME);
        newVersion.setVersionLabel("v" + (int) (Math.random() * 100000));
        newVersion.setStatus(ACTIVE_VERSION_STATUS);
        newVersion.setBaseVersionId(activeVersion.getId());
        newVersion.setCreatedAt(java.time.LocalDateTime.now());
        newVersion.setTotalComparisons(comparisons.size());
        newVersion.setAverageAccuracyPercent(averageScore * 100.0);
        newVersion.setNotes("Adaptive market weights updated from live comparison results");
        newVersion = marketModelVersionRepository.save(newVersion);

        activeVersion.setStatus(ARCHIVED_VERSION_STATUS);
        marketModelVersionRepository.save(activeVersion);

        for (Map.Entry<String, Double> entry : MARKET_SIGNAL_WEIGHTS.entrySet()) {
            MarketModelWeightEntity weightEntity = new MarketModelWeightEntity();
            weightEntity.setModelVersionId(newVersion.getId());
            weightEntity.setSignalName(entry.getKey());
            weightEntity.setWeightValue(entry.getValue());
            weightEntity.setCreatedAt(java.time.LocalDateTime.now());
            marketModelWeightRepository.save(weightEntity);
        }

        log.info("Market model version updated and persisted: {} with weights {}", newVersion.getVersionLabel(), MARKET_SIGNAL_WEIGHTS);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
