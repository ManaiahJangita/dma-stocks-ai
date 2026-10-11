package com.stockai.service.impl;

import com.stockai.entity.DashboardEntity;
import com.stockai.entity.MarketEntity;
import com.stockai.entity.PredictionEntity;
import com.stockai.service.impl.MarketAnalytics.Snapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Turns one year of market history and the latest predictions into dashboard rows,
 * one set of rows per dashboard filter option.
 */
@Component
public class DashboardDataBuilder {
    public static final String MARKET_SUMMARY = "market_summary";
    public static final String MARKET_MOVERS = "market_movers";
    public static final String MARKET_SIGNALS = "market_signals";
    public static final String SECTOR_PERFORMANCE = "sector_performance";

    public static final List<String> SECTOR_OPTIONS = List.of("Banking", "Information Technology",
            "Financial Services", "Automobile", "Pharmaceuticals", "Energy", "Metals", "FMCG", "Realty");

    /**
     * Instrument-driven inputs. An empty equity set means every symbol found in the market data is used;
     * indexes maps a summary option (e.g. "NIFTY 50") to the instrument name; sectors is optional membership.
     */
    public record Universe(Set<String> equities, Map<String, String> indexes, Map<String, Set<String>> sectors) {
        public static Universe all() {
            return new Universe(Set.of(), Map.of(), Map.of());
        }
    }

    static String key(String value) {
        return normalizeSymbol(value).replaceAll("[^A-Z0-9&]", "");
    }

    private static final double NEAR_52_WEEK_LEVEL = 0.02;

    public List<DashboardEntity> build(List<MarketEntity> markets, List<PredictionEntity> predictions,
                                       Universe universe, int limit, LocalDateTime now) {
        List<DashboardEntity> rows = new ArrayList<>();
        List<Snapshot> all = MarketAnalytics.snapshots(MarketAnalytics.seriesBySymbol(markets));
        List<Snapshot> snapshots = universe.equities().isEmpty() ? all
                : all.stream().filter(s -> universe.equities().contains(key(s.symbol()))).toList();
        if (!snapshots.isEmpty()) {
            addMovers(rows, snapshots, limit, now);
            addSummary(rows, snapshots, now);
            addSectors(rows, snapshots, universe.sectors(), now);
            addSignals(rows, snapshots, limit, now);
        }
        addIndexes(rows, all, universe.indexes(), now);
        List<PredictionEntity> scoped = universe.equities().isEmpty() ? predictions
                : predictions.stream().filter(p -> universe.equities().contains(key(p.getSymbol()))).toList();
        addPredictions(rows, scoped, limit, now);
        return rows;
    }

    private void addIndexes(List<DashboardEntity> rows, List<Snapshot> all, Map<String, String> indexes,
                            LocalDateTime now) {
        for (Map.Entry<String, String> index : indexes.entrySet()) {
            String indexKey = key(index.getValue());
            // Index rows only exist when index history was loaded into the markets table.
            all.stream().filter(s -> key(s.symbol()).equals(indexKey)).findFirst().ifPresent(s -> {
                DashboardEntity row = stockRow(MARKET_SUMMARY, index.getKey(), s, s.close(), 1,
                        s.changePercent() == null ? "Neutral" : s.changePercent() > 0 ? "Bullish"
                                : s.changePercent() < 0 ? "Bearish" : "Neutral",
                        index.getKey() + " index level", now);
                rows.add(row);
            });
        }
    }

    static String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        String value = symbol.trim().toUpperCase(Locale.ROOT);
        return value.endsWith("-EQ") ? value.substring(0, value.length() - 3) : value;
    }

    private void addMovers(List<DashboardEntity> rows, List<Snapshot> snapshots, int limit, LocalDateTime now) {
        addRanked(rows, MARKET_MOVERS, "Top Gainers", snapshots, limit, now,
                s -> s.changePercent() != null && s.changePercent() > 0,
                Comparator.comparing((Snapshot s) -> s.changePercent()).reversed(), Snapshot::changePercent);
        addRanked(rows, MARKET_MOVERS, "Top Losers", snapshots, limit, now,
                s -> s.changePercent() != null && s.changePercent() < 0,
                Comparator.comparing((Snapshot s) -> s.changePercent()), Snapshot::changePercent);
        addRanked(rows, MARKET_MOVERS, "Most Active by Volume", snapshots, limit, now,
                s -> s.volume() > 0, Comparator.comparingDouble(Snapshot::volume).reversed(), Snapshot::volume);
        addRanked(rows, MARKET_MOVERS, "Most Active by Value", snapshots, limit, now,
                s -> s.volume() > 0,
                Comparator.comparingDouble((Snapshot s) -> s.close() * s.volume()).reversed(),
                s -> round(s.close() * s.volume()));
        addRanked(rows, MARKET_MOVERS, "52-Week High", snapshots, limit, now,
                s -> s.close() >= s.high52() * (1 - NEAR_52_WEEK_LEVEL),
                Comparator.comparingDouble(s -> (s.high52() - s.close()) / s.high52()),
                s -> round((s.high52() - s.close()) / s.high52() * 100));
        addRanked(rows, MARKET_MOVERS, "52-Week Low", snapshots, limit, now,
                s -> s.low52() > 0 && s.close() <= s.low52() * (1 + NEAR_52_WEEK_LEVEL),
                Comparator.comparingDouble(s -> (s.close() - s.low52()) / s.low52()),
                s -> round((s.close() - s.low52()) / s.low52() * 100));
    }

    private void addRanked(List<DashboardEntity> rows, String type, String option, List<Snapshot> snapshots,
                           int limit, LocalDateTime now, Predicate<Snapshot> filter, Comparator<Snapshot> order,
                           java.util.function.Function<Snapshot, Double> metric) {
        List<Snapshot> selected = snapshots.stream().filter(filter).sorted(order).limit(limit).toList();
        for (int i = 0; i < selected.size(); i++) {
            Snapshot s = selected.get(i);
            rows.add(stockRow(type, option, s, metric.apply(s), i + 1, null, null, now));
        }
    }

    private void addSummary(List<DashboardEntity> rows, List<Snapshot> snapshots, LocalDateTime now) {
        LocalDate dataDate = snapshots.get(0).latest().getMarketDate();
        long advances = snapshots.stream().filter(s -> s.changePercent() != null && s.changePercent() > 0).count();
        long declines = snapshots.stream().filter(s -> s.changePercent() != null && s.changePercent() < 0).count();
        long unchanged = snapshots.stream().filter(s -> s.changePercent() != null && s.changePercent() == 0).count();
        long total = advances + declines + unchanged;
        long aboveAverage = snapshots.stream().filter(s -> s.sma20() != null && s.close() > s.sma20()).count();
        long withAverage = snapshots.stream().filter(s -> s.sma20() != null).count();

        rows.add(aggregateRow(MARKET_SUMMARY, "Advances vs Declines", "Advances vs Declines",
                declines == 0 ? (double) advances : round((double) advances / declines), null,
                advances > declines ? "Bullish" : advances < declines ? "Bearish" : "Neutral",
                "Advances: " + advances + ", Declines: " + declines + ", Unchanged: " + unchanged, dataDate, now));
        rows.add(aggregateRow(MARKET_SUMMARY, "Market Breadth", "Market Breadth",
                total == 0 ? 0.0 : round((double) advances / total * 100), null,
                advances > declines ? "Bullish" : advances < declines ? "Bearish" : "Neutral",
                "Advancing stocks: " + advances + "/" + total + ", above 20-day average: "
                        + aboveAverage + "/" + withAverage, dataDate, now));
    }

    private void addSectors(List<DashboardEntity> rows, List<Snapshot> snapshots, Map<String, Set<String>> groups,
                            LocalDateTime now) {
        LocalDate dataDate = snapshots.get(0).latest().getMarketDate();
        for (String sector : SECTOR_OPTIONS) {
            addGroupRow(rows, SECTOR_PERFORMANCE, sector, snapshots, groups.get(sector), dataDate, now);
        }
    }

    private void addGroupRow(List<DashboardEntity> rows, String type, String option, List<Snapshot> snapshots,
                             Set<String> members, LocalDate dataDate, LocalDateTime now) {
        if (members == null || members.isEmpty()) {
            return;
        }
        List<Snapshot> matched = snapshots.stream()
                .filter(s -> s.changePercent() != null && members.contains(key(s.symbol())))
                .toList();
        if (matched.isEmpty()) {
            return;
        }
        double average = round(matched.stream().mapToDouble(Snapshot::changePercent).average().orElse(0));
        long advances = matched.stream().filter(s -> s.changePercent() > 0).count();
        long declines = matched.stream().filter(s -> s.changePercent() < 0).count();
        rows.add(aggregateRow(type, option, option, average, average,
                average > 0 ? "Bullish" : average < 0 ? "Bearish" : "Neutral",
                "Equal-weighted average of " + matched.size() + " stocks, advances: " + advances
                        + ", declines: " + declines, dataDate, now));
    }

    private void addSignals(List<DashboardEntity> rows, List<Snapshot> snapshots, int limit, LocalDateTime now) {
        Predicate<Snapshot> bullish = s -> s.rsi() != null && s.sma20() != null && s.sma50() != null
                && s.close() > s.sma20() && s.sma20() > s.sma50() && s.rsi() > 50;
        Predicate<Snapshot> bearish = s -> s.rsi() != null && s.sma20() != null && s.sma50() != null
                && s.close() < s.sma20() && s.sma20() < s.sma50() && s.rsi() < 50;
        Predicate<Snapshot> neutral = s -> s.rsi() != null && s.sma50() != null && !bullish.test(s) && !bearish.test(s);

        addRanked(rows, MARKET_SIGNALS, "Bullish", snapshots, limit, now, bullish,
                Comparator.comparing((Snapshot s) -> s.rsi()).reversed(), Snapshot::rsi);
        addRanked(rows, MARKET_SIGNALS, "Bearish", snapshots, limit, now, bearish,
                Comparator.comparing((Snapshot s) -> s.rsi()), Snapshot::rsi);
        addRanked(rows, MARKET_SIGNALS, "Neutral", snapshots, limit, now, neutral,
                Comparator.comparingDouble(s -> Math.abs(s.rsi() - 50)), Snapshot::rsi);
        addRanked(rows, MARKET_SIGNALS, "Breakout", snapshots, limit, now, Snapshot::breakout,
                Comparator.comparingDouble((Snapshot s) -> s.changePercent() == null ? 0 : s.changePercent()).reversed(),
                Snapshot::rsi);
        addRanked(rows, MARKET_SIGNALS, "Overbought", snapshots, limit, now, s -> s.rsi() != null && s.rsi() >= 70,
                Comparator.comparing((Snapshot s) -> s.rsi()).reversed(), Snapshot::rsi);
        addRanked(rows, MARKET_SIGNALS, "Oversold", snapshots, limit, now, s -> s.rsi() != null && s.rsi() <= 30,
                Comparator.comparing((Snapshot s) -> s.rsi()), Snapshot::rsi);
    }

    private void addPredictions(List<DashboardEntity> rows, List<PredictionEntity> predictions, int limit,
                                LocalDateTime now) {
        if (predictions == null || predictions.isEmpty()) {
            return;
        }
        Comparator<PredictionEntity> byConfidence =
                Comparator.comparingDouble(PredictionEntity::getConfidencePercent).reversed();
        addPredictionRows(rows, "Prediction Up", predictions, p -> direction(p).equals("Up"), byConfidence, limit, now);
        addPredictionRows(rows, "Prediction Down", predictions, p -> direction(p).equals("Down"), byConfidence, limit, now);
        addPredictionRows(rows, "Prediction Sideways", predictions, p -> direction(p).equals("Sideways"), byConfidence,
                limit, now);
        addPredictionRows(rows, "High Confidence", predictions, p -> p.getConfidencePercent() >= 80, byConfidence,
                limit, now);
        addPredictionRows(rows, "Medium Confidence", predictions,
                p -> p.getConfidencePercent() >= 60 && p.getConfidencePercent() < 80, byConfidence, limit, now);
        addPredictionRows(rows, "Low Confidence", predictions, p -> p.getConfidencePercent() < 60, byConfidence,
                limit, now);
    }

    private static String direction(PredictionEntity prediction) {
        String value = prediction.getPredictionDirection() == null ? "" : prediction.getPredictionDirection().toLowerCase(Locale.ROOT);
        if (value.contains("up")) {
            return "Up";
        }
        return value.contains("down") ? "Down" : "Sideways";
    }

    private void addPredictionRows(List<DashboardEntity> rows, String option, List<PredictionEntity> predictions,
                                   Predicate<PredictionEntity> filter, Comparator<PredictionEntity> order, int limit,
                                   LocalDateTime now) {
        List<PredictionEntity> selected = predictions.stream().filter(filter).sorted(order).limit(limit)
                .collect(Collectors.toList());
        for (int i = 0; i < selected.size(); i++) {
            PredictionEntity p = selected.get(i);
            DashboardEntity row = new DashboardEntity();

            rows.add(row);
        }
    }

    private DashboardEntity stockRow(String type, String option, Snapshot s, Double metric, int rank, String status,
                                     String description, LocalDateTime now) {
        DashboardEntity row = new DashboardEntity();

        return row;
    }

    private DashboardEntity aggregateRow(String type, String option, String title, Double metric, Double change,
                                         String status, String description, LocalDate dataDate, LocalDateTime now) {
        DashboardEntity row = new DashboardEntity();
        return row;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
