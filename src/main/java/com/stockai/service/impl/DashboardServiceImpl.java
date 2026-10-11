package com.stockai.service.impl;

import com.stockai.dto.DashboardDto;
import com.stockai.dto.DashboardFilterDto;
import com.stockai.entity.*;
import com.stockai.repository.*;
import com.stockai.service.interf.DashboardService;
import com.stockai.utils.StockUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private HistoricalDashboardRepository  historicalDashboardRepository;


    @Override
    public List<DashboardDto> fetchDashBoardData(DashboardFilterDto filters) {
        List<DashboardEntity> dashboardData = dashboardRepository.findAllDashboardData(filters);
        return dashboardData.stream().map(this::convertEntityToDto).collect(Collectors.toList());
    }

    @Override
    public List<DashboardDto> fetchDashBoardSummary() {
        List<DashboardEntity> dashboardData = dashboardRepository.findAllDashboardSummary();
        return dashboardData.stream().map(this::convertEntityToDto).collect(Collectors.toList());
    }

    @Override
    public int processDashboardData() {
        return syncMarketAndPredictionData();
    }

    /**
     * Sync market and prediction data into dashboard table
     * Fetches all market data from last 2 years
     * Fetches all latest prediction data
     * Merges them by symbol
     * Calculates diffs (market - prediction)
     * Stores merged results in dashboard table
     */
    public int syncMarketAndPredictionData() {
        try {
            log.info("Starting market and prediction data sync...");
            dashboardRepository.deleteDashboardRecords();
            int count = marketRepository.getCount();
            int totalCount = 0;
            final int BATCH_SIZE = 30000;
            log.info("Deleted existing dashboard records and found {} market rows", count);

            for (int offset = 0; offset < count; offset += BATCH_SIZE) {
                int limit = Math.min(BATCH_SIZE, count - offset);
                log.info("Fetching market records batch: offset={} limit={}", offset, limit);

                List<MarketEntity> marketData = marketRepository.findAllMarketData(offset, limit);
                log.info("Fetched {} market records for this batch", marketData.size());

                if (marketData.isEmpty()) {
                    break;
                }

                List<PredictionEntity> predictionData = predictionRepository.findAllPredictions();
                log.info("Fetched {} prediction records", predictionData.size());

                Map<String, PredictionEntity> predictionMap = predictionData.stream().collect(Collectors.toMap(
                        PredictionEntity::getSymbol,
                        p -> p,
                        (existing, replacement) -> replacement
                ));

                List<DashboardEntity> dashboardRecords = new ArrayList<>();
                for (MarketEntity market : marketData) {
                    DashboardEntity dashboard = new DashboardEntity();
                    copyMarketToDashboard(market, dashboard);

                    PredictionEntity prediction = predictionMap.get(market.getSymbol());
                    if (prediction != null) {
                        copyPredictionToDashboard(prediction, dashboard);
                        calculateDifferences(market, prediction, dashboard);
                    } else {
                        dashboard.setPredictedHigh(null);
                        dashboard.setPredictedLow(null);
                        dashboard.setPredictionClose(null);
                        dashboard.setMarketPredictionHighDiff(null);
                        dashboard.setMarketPredictionLowDiff(null);
                        dashboard.setPredictionDirection(null);
                        dashboard.setConfidencePercent(null);
                    }

                    dashboard.setMarketDataType("MARKET_PREDICTION");
                    dashboardRecords.add(dashboard);
                }

                log.info("Merged {} market and prediction records in this batch", dashboardRecords.size());
                for (int j = 0; j < dashboardRecords.size(); j += 500) {
                    int storeEnd = Math.min(j + 500, dashboardRecords.size());
                    List<DashboardEntity> batch = dashboardRecords.subList(j, storeEnd);
                    log.info("Saving batch of {} dashboard records", batch.size());
                    dashboardRepository.replaceDashboardData(batch);
                }
                List<HistoricalDashboardEntity> historicalRecords = dashboardRecords.stream().map(d -> {
                    HistoricalDashboardEntity historical = new HistoricalDashboardEntity();
                    BeanUtils.copyProperties(d, historical);
                    return historical;
                }).collect(Collectors.toList());
                historicalDashboardRepository.saveAllHistoricalDashboards(historicalRecords);
                log.info("Saved {} historical dashboard records", historicalRecords.size());
                totalCount += dashboardRecords.size();
                marketData.clear();
                dashboardRecords.clear();
            }
            return totalCount;
        } catch (Exception e) {
            log.error("Error while syncing dashboard data", e);
            return 0;
        }
    }

    /**
     * Copy market data to dashboard entity
     */
    private void copyMarketToDashboard(MarketEntity market, DashboardEntity dashboard) {
        dashboard.setSymbol(market.getSymbol());
        dashboard.setName(market.getName());
        dashboard.setStatus(market.getStatus());

        // Price data
        dashboard.setOpen(market.getOpen());
        dashboard.setHigh(market.getHigh());
        dashboard.setLow(market.getLow());
        dashboard.setClose(market.getClose());

        // Volume and percent data
        dashboard.setVolume(market.getVolume());
        dashboard.setPer(market.getPer());
        dashboard.setPtsC(market.getPtsC());

        // Dates
        dashboard.setHistoricalCreatedDate(market.getHistoricalCreatedDate());
        dashboard.setHistoricalUpdateDate(market.getHistoricalUpdateDate());
        dashboard.setCreatedDate(market.getCreatedDate());
        dashboard.setUpdatedDate(market.getUpdatedDate());

        // Market analytics
        dashboard.setMarketDate(market.getMarketDate());
        dashboard.setPreviousClose(market.getPreviousClose());
        dashboard.setChangePercent(market.getChangePercent());

        // Technical indicators
        dashboard.setAvgVolume(market.getAvgVolume());
        dashboard.setSma5(market.getSma5());
        dashboard.setSma10(market.getSma10());
        dashboard.setSma20(market.getSma20());
        dashboard.setSma50(market.getSma50());
        dashboard.setEma12(market.getEma12());
        dashboard.setEma26(market.getEma26());
        dashboard.setRsi14(market.getRsi14());

        // Analytics fields
        dashboard.setVolumeRatio(market.getVolumeRatio());
        dashboard.setReturn1d(market.getReturn1d());
        dashboard.setReturn5d(market.getReturn5d());
        dashboard.setReturn10d(market.getReturn10d());
        dashboard.setReturn30d(market.getReturn30d());
        dashboard.setDistanceFrom52wHighPct(market.getDistanceFrom52wHighPct());
        dashboard.setDistanceFrom52wLowPct(market.getDistanceFrom52wLowPct());
        dashboard.setDistanceFromSma20Pct(market.getDistanceFromSma20Pct());

        // Flags and signals
        dashboard.setBreakoutFlag(market.getBreakoutFlag());
        dashboard.setTrendLabel(market.getTrendLabel());

        // Next day predictions from market analytics
        dashboard.setNextDayHigh(market.getNextDayHigh());
        dashboard.setNextDayReturnPct(market.getNextDayReturnPct());
        dashboard.setNextDayLow(market.getNextDayLow());
        dashboard.setNextDayLowReturnPct(market.getNextDayLowReturnPct());

        // Confidence from market analytics
        dashboard.setConfidenceScore(market.getConfidenceScore());
    }

    /**
     * Copy prediction data to dashboard entity
     */
    private void copyPredictionToDashboard(PredictionEntity prediction, DashboardEntity dashboard) {
        dashboard.setPredictedHigh(prediction.getPredictedHigh());
        dashboard.setPredictedLow(prediction.getPredictedLow());
        dashboard.setPredictionClose(prediction.getPredictionClose());
        dashboard.setPredictionDirection(prediction.getPredictionDirection());
        dashboard.setConfidencePercent(prediction.getConfidencePercent());
    }

    /**
     * Calculate differences between market and prediction values
     * For market-prediction analysis and comparison
     */
    private void calculateDifferences(MarketEntity market, PredictionEntity prediction, DashboardEntity dashboard) {
        // Market High - Predicted High
        if (market.getHigh() != null) {
            Double highDiff = market.getHigh() - prediction.getPredictedHigh();
            dashboard.setMarketPredictionHighDiff(highDiff);
        }

        // Market Low - Predicted Low
        if (market.getLow() != null) {
            Double lowDiff = market.getLow() - prediction.getPredictedLow();
            dashboard.setMarketPredictionLowDiff(lowDiff);
        }
        if (market.getConfidenceScore() != null) {
            Double marketConfidence = market.getConfidenceScore();
            Double predictionConfidence = prediction.getConfidencePercent();
            Double confidenceDiff = marketConfidence - predictionConfidence;
            dashboard.setConfidenceScoreDiff(confidenceDiff);

            log.debug("Confidence Diff - Market: {}%, Prediction: {}%, Diff: {}%",
                marketConfidence, predictionConfidence, confidenceDiff);
        }
    }

    /**
     * Convert dashboard entity to DTO
     */
    private DashboardDto convertEntityToDto(DashboardEntity entity) {
        DashboardDto dto = new DashboardDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
