package com.stockai.schedulars;

import com.stockai.entity.HistoricalPatternEntity;
import com.stockai.entity.MarketEntity;
import com.stockai.entity.PatternEntity;
import com.stockai.repository.MarketRepository;
import com.stockai.repository.PatternsRepository;
import com.stockai.utils.StockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PatternSyncScheduler {

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private PatternsRepository patternsRepository;

    @Scheduled(cron = "0 10 19 ? * MON-FRI")
    public void executeJob() {
        if (!StockUtil.isTradingDay()) {
            return;
        }
        try {
            int saved = syncTwoYearPatterns();
            log.info("Pattern sync completed. Saved {} rows", saved);
        } catch (Exception e) {
            log.error("Error while syncing pattern data", e);
        }
    }

    public synchronized int syncTwoYearPatterns() {
        patternsRepository.truncatePatternRecords();
        List<MarketEntity> marketRows = new ArrayList<>(marketRepository.findAllMarketData());
        if (marketRows == null || marketRows.isEmpty()) {
            log.warn("No market rows available for pattern calculation ");
            return 0;
        }

        if (marketRows.isEmpty()) {
            log.warn("No market history found in date range {} to {}", LocalDate.now().minusYears(2), LocalDate.now());
            return 0;
        }

        Map<String, List<MarketEntity>> bySymbol = new HashMap<>();
        for (MarketEntity row : marketRows) {
            if (row.getSymbol() == null || row.getClose() == null || row.getMarketDate() == null) {
                continue;
            }
            bySymbol.computeIfAbsent(row.getSymbol(), key -> new ArrayList<>()).add(row);
        }

        List<PatternEntity> patternRows = new ArrayList<>();
        for (Map.Entry<String, List<MarketEntity>> entry : bySymbol.entrySet()) {
            List<MarketEntity> series = entry.getValue();
            series.sort(Comparator.comparing(MarketEntity::getMarketDate));
            MarketEntity latest = series.get(0);
            double changePercent = latest.getPtsC() == null || latest.getClose() == null || latest.getClose() == 0
                    ? 0 : (latest.getPtsC() / latest.getClose()) * 100;
            double volumeRatio = latest.getVolume() != null && latest.getAvgVolume() != null && latest.getAvgVolume() > 0
                    ? latest.getVolume().doubleValue() / latest.getAvgVolume() : 0;
            double sma20 = latest.getSma20() == null ? 0 : latest.getSma20();
            double sma50 = latest.getSma50() == null ? 0 : latest.getSma50();
            double rsi14 = latest.getRsi14() == null ? 0 : latest.getRsi14();
            String trendLabel = latest.getTrendLabel() == null ? "Neutral" : latest.getTrendLabel();
            boolean breakoutFlag = Boolean.TRUE.equals(latest.getBreakoutFlag());

            if (breakoutFlag && volumeRatio >= 1.2 && "Bullish".equalsIgnoreCase(trendLabel)) {
                patternRows.add(buildPatternRow(
                        latest,
                        "Breakout with Volume Confirmation",
                        "Bullish",
                        "Breakout",
                        "bullish_breakout",
                        changePercent,
                        volumeRatio,
                        sma20,
                        sma50,
                        rsi14,
                        trendLabel,
                        true,
                        Math.max(0, Math.min(100, 50 + (rsi14 - 50) * 0.8 + (volumeRatio - 1) * 30))
                ));
            }

            if (latest.getClose() > sma20 && sma20 > sma50 && rsi14 > 50 && "Bullish".equalsIgnoreCase(trendLabel)) {
                patternRows.add(buildPatternRow(
                        latest,
                        "Trend Pullback",
                        "Bullish",
                        "Trend",
                        "trend_pullback",
                        changePercent,
                        volumeRatio,
                        sma20,
                        sma50,
                        rsi14,
                        trendLabel,
                        breakoutFlag,
                        Math.max(0, Math.min(100, 55 + (rsi14 - 50) * 0.5 + (latest.getClose() - sma20) / Math.max(sma20, 1) * 100))
                ));
            }

            if (rsi14 > 50 && rsi14 < 70 && "Neutral".equalsIgnoreCase(trendLabel)) {
                patternRows.add(buildPatternRow(
                        latest,
                        "Momentum Shift",
                        "Bullish",
                        "Momentum",
                        "momentum_shift",
                        changePercent,
                        volumeRatio,
                        sma20,
                        sma50,
                        rsi14,
                        trendLabel,
                        breakoutFlag,
                        Math.max(0, Math.min(100, 60 + (rsi14 - 50) * 0.7))
                ));
            }
        }

        if (patternRows.isEmpty()) {
            log.warn("No pattern rows detected for the market data in date");
            return 0;
        }

        patternRows.sort(Comparator.comparing(PatternEntity::getScore, Comparator.nullsLast(Double::compareTo)).reversed());
        int saved = patternsRepository.replacePatterns(patternRows);
        List<HistoricalPatternEntity> historicalPatterns = patternRows.stream().map(pattern -> {
            HistoricalPatternEntity historicalPattern = new HistoricalPatternEntity();
            historicalPattern.setName(pattern.getName());
            historicalPattern.setPatternName(pattern.getPatternName());
            historicalPattern.setPatternType(pattern.getPatternType());
            historicalPattern.setSignalType(pattern.getSignalType());
            historicalPattern.setFilterValue(pattern.getFilterValue());
            historicalPattern.setFilterType(pattern.getFilterType());
            historicalPattern.setSymbol(pattern.getSymbol());
            historicalPattern.setStatus(pattern.getStatus());
            historicalPattern.setDescription(pattern.getDescription());
            historicalPattern.setMetricValue(pattern.getMetricValue());
            historicalPattern.setChangePercent(pattern.getChangePercent());
            historicalPattern.setClosePrice(pattern.getClosePrice());
            historicalPattern.setVolume(pattern.getVolume());
            historicalPattern.setRankNo(pattern.getRankNo());
            historicalPattern.setDataDate(pattern.getDataDate());
            historicalPattern.setCreatedDate(LocalDateTime.now());
            historicalPattern.setVolumeRatio(pattern.getVolumeRatio());
            historicalPattern.setSma20(pattern.getSma20());
            historicalPattern.setSma50(pattern.getSma50());
            historicalPattern.setRsi14(pattern.getRsi14());
            historicalPattern.setTrendLabel(pattern.getTrendLabel());
            historicalPattern.setScore(pattern.getScore());
            return historicalPattern;
        }).collect(Collectors.toList());
        patternsRepository.saveAllHistoricalPatterns(historicalPatterns);
        log.info("Saved pattern rows for {} ", saved);
        return saved;
    }

    private PatternEntity buildPatternRow(
            MarketEntity latest,
            String patternName,
            String status,
            String patternType,
            String filterValue,
            double changePercent,
            double volumeRatio,
            double sma20,
            double sma50,
            double rsi14,
            String trendLabel,
            boolean breakoutFlag,
            double score) {
        PatternEntity entity = new PatternEntity();
        entity.setName(patternName);
        entity.setPatternName(patternName);
        entity.setPatternType(patternType);
        entity.setSignalType(status);
        entity.setFilterValue(filterValue);
        entity.setFilterType("pattern");
        entity.setSymbol(latest.getSymbol());
        entity.setStatus(status);
        entity.setDescription(patternName + " for " + latest.getSymbol() + " on " + latest.getMarketDate());
        entity.setMetricValue(score);
        entity.setChangePercent(changePercent);
        entity.setClosePrice(latest.getClose());
        entity.setVolume(latest.getVolume());
        entity.setRankNo(1);
        entity.setDataDate(latest.getMarketDate());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setVolumeRatio(volumeRatio);
        entity.setSma20(sma20);
        entity.setSma50(sma50);
        entity.setRsi14(rsi14);
        entity.setTrendLabel(trendLabel);
        entity.setBreakoutFlag(breakoutFlag);
        entity.setScore(score);
        return entity;
    }
}

