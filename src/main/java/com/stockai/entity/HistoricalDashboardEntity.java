package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Historical snapshot of dashboard data. Each row represents a dashboard snapshot saved at a specific time.
 * Useful for auditing and analyzing how dashboard data changes over time.
 * Mapped to dmastock.historical_dashboard table.
 */
@Data
@Entity
@Table(name = "historical_dashboard", schema = "dmastock")
public class HistoricalDashboardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String status;

    @Column(name = "symbol", length = 60)
    private String symbol;

    @Column(name = "open")
    private Double open;

    @Column(name = "high")
    private Double high;

    @Column(name = "low")
    private Double low;

    @Column(name = "close")
    private Double close;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "per")
    private Double per;

    @Column(name = "pts_c")
    private Double ptsC;

    @Column(name = "historical_created_date", nullable = false)
    private LocalDateTime historicalCreatedDate;

    @Column(name = "historical_update_date", nullable = false)
    private LocalDate historicalUpdateDate;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    @Column(name = "market_date")
    private LocalDate marketDate;

    @Column(name = "previous_close")
    private Double previousClose;

    @Column(name = "change_percent")
    private Double changePercent;

    @Column(name = "avg_volume")
    private Long avgVolume;

    @Column(name = "sma_5")
    private Double sma5;

    @Column(name = "sma_10")
    private Double sma10;

    @Column(name = "sma_20")
    private Double sma20;

    @Column(name = "sma_50")
    private Double sma50;

    @Column(name = "ema_12")
    private Double ema12;

    @Column(name = "ema_26")
    private Double ema26;

    @Column(name = "rsi_14")
    private Double rsi14;

    @Column(name = "volume_ratio")
    private Double volumeRatio;

    @Column(name = "return_1d")
    private Double return1d;

    @Column(name = "return_5d")
    private Double return5d;

    @Column(name = "return_10d")
    private Double return10d;

    @Column(name = "return_30d")
    private Double return30d;

    @Column(name = "distance_from_52w_high_pct")
    private Double distanceFrom52wHighPct;

    @Column(name = "distance_from_52w_low_pct")
    private Double distanceFrom52wLowPct;

    @Column(name = "distance_from_sma20_pct")
    private Double distanceFromSma20Pct;

    @Column(name = "breakout_flag")
    private Boolean breakoutFlag;

    @Column(name = "trend_label", length = 40)
    private String trendLabel;

    @Column(name = "next_day_high")
    private Double nextDayHigh;

    @Column(name = "next_day_low")
    private Double nextDayLow;

    @Column(name = "next_day_return_pct")
    private Double nextDayReturnPct;

    @Column(name = "next_day_low_return_pct")
    private Double nextDayLowReturnPct;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "predicted_high")
    private Double predictedHigh;

    @Column(name = "predicted_low")
    private Double predictedLow;

    @Column(name = "prediction_close")
    private Double predictionClose;

    @Column(name = "market_prediction_high_diff")
    private Double marketPredictionHighDiff;

    @Column(name = "market_prediction_low_diff")
    private Double marketPredictionLowDiff;

    @Column(name = "prediction_direction", length = 30)
    private String predictionDirection;

    @Column(name = "confidence_percent")
    private Double confidencePercent;

    @Column(name = "confidence_score_diff")
    private Double confidenceScoreDiff;

    @Column(name = "market_data_type", length = 30)
    private String marketDataType;

}

