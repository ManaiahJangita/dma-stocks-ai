package com.stockai.service.impl;

import com.stockai.dto.MarketDto;
import com.stockai.dto.MarketFiltersDto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.HistoricalMarketEntity;
import com.stockai.entity.MarketEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.MarketRepository;
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
    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private HistoricalDataRepository historicalDataRepository;


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
        LocalDate startDate = endDate.minusYears(2);

        try {
            log.info("Starting historical data sync from {} to {}", startDate, endDate);
            marketRepository.deleteHistoricalMarkets();
            log.info("Deleted existing historical markets data");

            log.info("Fetching historical data from {} through {}", startDate, endDate);
            List<HistoricalDataEntity> history = historicalDataRepository.findAll(startDate, endDate);
            log.info("Fetched {} historical data records", history.size());

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
            }

            log.info("Transformed {} historical entities to market entities", marketRows.size());

            if (!marketRows.isEmpty()) {
                log.info("Enriching market analytics for {} rows", marketRows.size());
                enrichMarketAnalytics(marketRows);
                log.info("Completed enriching market analytics");

                int totalUpserted = 0;
                for(int i = 0; i < marketRows.size(); i += 500) {
                    int end = Math.min(i + 500, marketRows.size());
                    List<MarketEntity> batch = marketRows.subList(i, end);
                    log.info("Upserting batch {} - rows {} to {} (batch size: {})",
                            (i/500 + 1), i, end, batch.size());

                    try {
                        marketRepository.upsertHistoricalMarkets(batch);
                        log.info("Successfully upserted {} market rows to market table", batch.size());
                        totalUpserted += batch.size();
                    } catch (Exception e) {
                        log.error("Error upserting market batch at offset {}", i, e);
                        throw e;
                    }

                    List<HistoricalMarketEntity> historicalBatch = new ArrayList<>();
                    for (MarketEntity market : batch) {
                        HistoricalMarketEntity historicalMarket = new HistoricalMarketEntity();
                        BeanUtils.copyProperties(market, historicalMarket);
                        historicalBatch.add(historicalMarket);
                    }

                    try {
                        marketRepository.upsertHistoricalMarketsEveryday(historicalBatch);
                        log.info("Successfully upserted {} rows to historical_market_everyday table",
                                historicalBatch.size());
                    } catch (Exception e) {
                        log.error("Error upserting historical market batch at offset {}", i, e);
                        throw e;
                    }
                }
                log.info("Completed sync: Total {} market rows upserted in {} batches",
                        totalUpserted, (marketRows.size() + 499) / 500);
                return totalUpserted;
            }
            return 0;
        } catch (Exception e) {
            log.error("Fatal error during historical data sync", e);
            throw e;
        }
    }

    private void enrichMarketAnalytics(List<MarketEntity> marketRows) {
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

            // Calculate next day predictions
            double nextMove = latest.getChangePercent() == null ? 0.0 : latest.getChangePercent();
            double nextDayHigh = latest.getClose() * (1 + (nextMove / 100.0));
            latest.setNextDayHigh(nextDayHigh);
            latest.setNextDayReturnPct(nextMove);

            // Calculate confidence score based on multiple technical factors
            double confidence = 50.0;
            if (latest.getRsi14() != null) {
                confidence += (latest.getRsi14() - 50) * 0.75;
            }
            if (Boolean.TRUE.equals(latest.getBreakoutFlag())) {
                confidence += 15;
            }
            if (latest.getVolumeRatio() != null) {
                confidence += Math.min(20, latest.getVolumeRatio() * 10);
            }
            if (latest.getSma20() != null && latest.getClose() != null && latest.getClose() > latest.getSma20()) {
                confidence += 10;
            }
            confidence = Math.max(0, Math.min(100, confidence));
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
}
