package com.stockai.service.impl;

import com.stockai.entity.MarketEntity;
import com.stockai.entity.PredictionEntity;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

/**
 * Shared calculations over the market history rows (one year of daily candles per symbol).
 */
final class MarketAnalytics {

    /** Same sector names for every screen; membership is configured with dashboard.sector.* properties. */
    static final List<String> SECTOR_OPTIONS = List.of("Banking", "Information Technology", "Financial Services",
            "Automobile", "Pharmaceuticals", "Healthcare", "Energy", "Metals", "FMCG", "Realty", "Telecom","Gold");

    record Snapshot(String symbol, MarketEntity latest, double close, Double changePercent, double volume,
                    Double rsi, Double sma20, Double sma50, double high52, double low52, boolean breakout) {
    }

    /** One generated result row; each screen maps it onto its own table. */
    record AnalysisRow(String filterType, String filterValue, String symbol, String title, String status,
                       String description, Double metric, Double changePercent, Double close, Long volume,
                       int rank) {
    }

    private MarketAnalytics() {
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static double open(MarketEntity m) {
        return m.getOpen() == null ? m.getClose() : m.getOpen();
    }

    static double high(MarketEntity m) {
        return m.getHigh() == null ? m.getClose() : m.getHigh();
    }

    static double low(MarketEntity m) {
        return m.getLow() == null ? m.getClose() : m.getLow();
    }

    /** Series per symbol sorted by date, keeping only symbols that traded on the latest date. */
    static Map<String, List<MarketEntity>> seriesBySymbol(List<MarketEntity> markets) {
        Map<String, List<MarketEntity>> bySymbol = new HashMap<>();
        LocalDate latestDate = null;
        for (MarketEntity market : markets) {
            if (market.getSymbol() == null || market.getClose() == null || market.getMarketDate() == null) {
                continue;
            }
            bySymbol.computeIfAbsent(market.getSymbol(), key -> new ArrayList<>()).add(market);
            if (latestDate == null || market.getMarketDate().isAfter(latestDate)) {
                latestDate = market.getMarketDate();
            }
        }
        for (List<MarketEntity> series : bySymbol.values()) {
            series.sort(Comparator.comparing(MarketEntity::getMarketDate));
        }
        LocalDate latest = latestDate;
        bySymbol.values().removeIf(series -> !series.get(series.size() - 1).getMarketDate().equals(latest));
        return bySymbol;
    }

    static Map<String, List<MarketEntity>> restrict(Map<String, List<MarketEntity>> series, Set<String> equities) {
        if (equities.isEmpty()) {
            return series;
        }
        Map<String, List<MarketEntity>> scoped = new HashMap<>();
        series.forEach((symbol, rows) -> {
            if (equities.contains(DashboardDataBuilder.key(symbol))) {
                scoped.put(symbol, rows);
            }
        });
        return scoped;
    }

    static List<Snapshot> snapshots(Map<String, List<MarketEntity>> bySymbol) {
        List<Snapshot> snapshots = new ArrayList<>();
        for (Map.Entry<String, List<MarketEntity>> entry : bySymbol.entrySet()) {
            List<MarketEntity> series = entry.getValue();
            MarketEntity latest = series.get(series.size() - 1);
            double close = latest.getClose();
            Double previous = series.size() > 1 ? series.get(series.size() - 2).getClose() : latest.getPreviousClose();
            Double change = previous != null && previous > 0 ? (close - previous) / previous * 100 : null;
            double volume = latest.getVolume() == null ? 0 : latest.getVolume();
            double high52 = series.stream().mapToDouble(MarketAnalytics::high).max().orElse(close);
            double low52 = series.stream().mapToDouble(MarketAnalytics::low).min().orElse(close);
            snapshots.add(new Snapshot(entry.getKey(), latest, close, change == null ? null : round(change), volume,
                    rsi(series, 14), sma(series, 20), sma(series, 50), high52, low52, breakout(series, 20)));
        }
        return snapshots;
    }

    static Double smaAt(List<MarketEntity> series, int endExclusive, int period) {
        if (endExclusive < period || endExclusive > series.size()) {
            return null;
        }
        return series.subList(endExclusive - period, endExclusive).stream()
                .mapToDouble(MarketEntity::getClose).average().orElse(0);
    }

    static Double sma(List<MarketEntity> series, int period) {
        return smaAt(series, series.size(), period);
    }

    static Double rsi(List<MarketEntity> series, int period) {
        if (series.size() <= period) {
            return null;
        }
        double gain = 0;
        double loss = 0;
        for (int i = 1; i <= period; i++) {
            double diff = series.get(i).getClose() - series.get(i - 1).getClose();
            gain += Math.max(diff, 0);
            loss += Math.max(-diff, 0);
        }
        double averageGain = gain / period;
        double averageLoss = loss / period;
        for (int i = period + 1; i < series.size(); i++) {
            double diff = series.get(i).getClose() - series.get(i - 1).getClose();
            averageGain = (averageGain * (period - 1) + Math.max(diff, 0)) / period;
            averageLoss = (averageLoss * (period - 1) + Math.max(-diff, 0)) / period;
        }
        if (averageLoss == 0) {
            return averageGain == 0 ? 50.0 : 100.0;
        }
        return round(100 - 100 / (1 + averageGain / averageLoss));
    }

    /** Highest high of the {@code lookback} candles before the latest one. */
    static Double priorHigh(List<MarketEntity> series, int lookback) {
        int n = series.size();
        if (lookback < 1 || n <= lookback) {
            return null;
        }
        return series.subList(n - 1 - lookback, n - 1).stream().mapToDouble(MarketAnalytics::high).max().orElse(0);
    }

    static Double priorLow(List<MarketEntity> series, int lookback) {
        int n = series.size();
        if (lookback < 1 || n <= lookback) {
            return null;
        }
        return series.subList(n - 1 - lookback, n - 1).stream().mapToDouble(MarketAnalytics::low).min().orElse(0);
    }

    static boolean breakout(List<MarketEntity> series, int period) {
        Double priorHigh = priorHigh(series, period);
        return priorHigh != null && series.get(series.size() - 1).getClose() > priorHigh;
    }

    static boolean breakdown(List<MarketEntity> series, int period) {
        Double priorLow = priorLow(series, period);
        return priorLow != null && series.get(series.size() - 1).getClose() < priorLow;
    }

    /** True when the latest close moved from at/below to above its simple moving average. */
    static boolean crossedAboveAverage(List<MarketEntity> series, int period) {
        int n = series.size();
        Double previous = smaAt(series, n - 1, period);
        Double current = smaAt(series, n, period);
        return previous != null && current != null
                && series.get(n - 2).getClose() <= previous && series.get(n - 1).getClose() > current;
    }

    /** 1 for a bullish MACD (12, 26, 9) crossover on the latest candle, -1 for bearish, 0 otherwise. */
    static int macdCrossover(List<MarketEntity> series) {
        int n = series.size();
        if (n < 35) {
            return 0;
        }
        double fast = series.get(0).getClose();
        double slow = fast;
        double signal = 0;
        double previousDiff = 0;
        double currentDiff = 0;
        for (int i = 0; i < n; i++) {
            double close = series.get(i).getClose();
            fast = close * (2.0 / 13) + fast * (1 - 2.0 / 13);
            slow = close * (2.0 / 27) + slow * (1 - 2.0 / 27);
            double macd = fast - slow;
            signal = i == 0 ? macd : macd * (2.0 / 10) + signal * (1 - 2.0 / 10);
            previousDiff = currentDiff;
            currentDiff = macd - signal;
        }
        if (previousDiff <= 0 && currentDiff > 0) {
            return 1;
        }
        return previousDiff >= 0 && currentDiff < 0 ? -1 : 0;
    }

    /** Percentage change of the latest close against the last close on or before the given number of days ago. */
    static Double changeOver(List<MarketEntity> series, int calendarDays) {
        int n = series.size();
        if (n < 2) {
            return null;
        }
        MarketEntity last = series.get(n - 1);
        LocalDate target = last.getMarketDate().minusDays(calendarDays);
        MarketEntity base = null;
        for (int i = n - 2; i >= 0; i--) {
            if (!series.get(i).getMarketDate().isAfter(target)) {
                base = series.get(i);
                break;
            }
        }
        if (base == null && calendarDays >= 360) {
            base = series.get(0);
        }
        if (base == null || base.getClose() <= 0) {
            return null;
        }
        return round((last.getClose() - base.getClose()) / base.getClose() * 100);
    }

    /** Standard deviation of daily percentage returns over the last {@code period} candles. */
    static Double volatility(List<MarketEntity> series, int period) {
        int n = series.size();
        if (n <= period) {
            return null;
        }
        double[] returns = new double[period];
        for (int i = 0; i < period; i++) {
            double previous = series.get(n - period - 1 + i).getClose();
            double current = series.get(n - period + i).getClose();
            returns[i] = previous > 0 ? (current - previous) / previous * 100 : 0;
        }
        double mean = Arrays.stream(returns).average().orElse(0);
        double variance = Arrays.stream(returns).map(r -> (r - mean) * (r - mean)).average().orElse(0);
        return round(Math.sqrt(variance));
    }

    /** Average volume of the {@code period} candles before the latest one. */
    static double averageVolume(List<MarketEntity> series, int period) {
        int n = series.size();
        int from = Math.max(0, n - 1 - period);
        return series.subList(from, n - 1).stream()
                .mapToLong(m -> m.getVolume() == null ? 0 : m.getVolume()).average().orElse(0);
    }

    static Double previousClose(List<MarketEntity> series) {
        return series.size() > 1 ? series.get(series.size() - 2).getClose() : series.get(0).getPreviousClose();
    }

    static String trend(Snapshot s) {
        if (s.rsi() == null || s.sma20() == null || s.sma50() == null) {
            return null;
        }
        if (s.close() > s.sma20() && s.sma20() > s.sma50() && s.rsi() > 50) {
            return "Bullish";
        }
        if (s.close() < s.sma20() && s.sma20() < s.sma50() && s.rsi() < 50) {
            return "Bearish";
        }
        return "Neutral";
    }

    static String direction(double changePercent) {
        return changePercent > 0 ? "Up" : changePercent < 0 ? "Down" : "Unchanged";
    }

    static String predictionDirection(PredictionEntity prediction) {
        String value = prediction.getPredictionDirection() == null ? ""
                : prediction.getPredictionDirection().toLowerCase(Locale.ROOT);
        if (value.contains("up")) {
            return "Up";
        }
        return value.contains("down") ? "Down" : "Sideways";
    }

    static AnalysisRow stockRow(String type, String option, Snapshot s, Double metric, int rank, String status,
                                String description) {
        String state = status != null ? status : s.changePercent() == null ? null : direction(s.changePercent());
        return new AnalysisRow(type, option, s.symbol(), s.symbol(), state, description,
                metric == null ? null : round(metric), s.changePercent(), round(s.close()), (long) s.volume(), rank);
    }

    static AnalysisRow summaryRow(String type, String option, String title, Double metric, Double change,
                                  String status, String description, LocalDate dataDate) {
        return new AnalysisRow(type, option, null, title, status, description, metric, change, null, null, 1);
    }

    static void rank(List<AnalysisRow> rows, String type, String option, List<Snapshot> snapshots, int limit,
                     Predicate<Snapshot> filter, Comparator<Snapshot> order,
                     java.util.function.Function<Snapshot, Double> metric, java.util.function.Function<Snapshot, String> status,
                     java.util.function.Function<Snapshot, String> description) {
        List<Snapshot> selected = snapshots.stream().filter(filter).sorted(order).limit(limit).toList();
        for (int i = 0; i < selected.size(); i++) {
            Snapshot s = selected.get(i);
            rows.add(stockRow(type, option, s, metric.apply(s), i + 1, status == null ? null : status.apply(s),
                    description == null ? null : description.apply(s)));
        }
    }

    static void rank(List<AnalysisRow> rows, String type, String option, List<Snapshot> snapshots, int limit,
                     Predicate<Snapshot> filter, Comparator<Snapshot> order,
                     java.util.function.Function<Snapshot, Double> metric) {
        rank(rows, type, option, snapshots, limit, filter, order, metric, null, null);
    }

    static void predictionRows(List<AnalysisRow> rows, String type, String option, List<PredictionEntity> predictions,
                               Predicate<PredictionEntity> filter, int limit) {
        List<PredictionEntity> selected = predictions.stream().filter(filter)
                .sorted(Comparator.comparingDouble(PredictionEntity::getConfidencePercent).reversed())
                .limit(limit).toList();
        for (int i = 0; i < selected.size(); i++) {
            PredictionEntity p = selected.get(i);
            rows.add(new AnalysisRow(type, option, p.getSymbol(), p.getSymbol(), p.getPredictionDirection(),
                    "Predicted close " + round(p.getPredictionClose()) + " on " + p.getPredictionDate()
                            + ", up probability " + round(p.getUpProbabilityPercent()) + "%",
                    round(p.getConfidencePercent()), round(p.getPredictionClosePercentage()),
                    round(p.getPredictionClose()), p.getVolume() == null ? null : p.getVolume().longValue(),
                    i + 1));
        }
    }
}
