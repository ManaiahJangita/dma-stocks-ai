package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "stock_analyzer", schema = "dmastock")
public class StockAnalyzerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "prediction_id", unique = true)
    private Long predictionId;

    @Column(name = "prediction_run_id")
    private UUID predictionRunId;

    private Double open;
    private Double high;
    private Double low;

    @Column(name = "latest_close")
    private Double latestClose;

    @Column(name = "previous_close")
    private Double previousClose;

    @Column(name = "trading_volume")
    private Double tradingVolume;

    private Double per;
    private Double ptsC;

    @Column(name = "history_date")
    private LocalDate historyDate;

    @Column(name = "prediction_date")
    private LocalDate predictionDate;

    @Column(name = "prediction_close")
    private Double predictionClose;

    @Column(name = "prediction_direction")
    private String predictionDirection;

    @Column(name = "prediction_close_percentage")
    private Double predictionClosePercentage;

    @Column(name = "predicted_high")
    private Double predictedHigh;

    @Column(name = "predicted_high_percentage")
    private Double predictedHighPercentage;

    @Column(name = "predicted_low")
    private Double predictedLow;

    @Column(name = "predicted_low_percentage")
    private Double predictedLowPercentage;

    @Column(name = "confidence_percent")
    private Double confidencePercent;

    @Column(name = "up_probability_percent")
    private Double upProbabilityPercent;

    @Column(name = "down_probability_percent")
    private Double downProbabilityPercent;

    @Column(name = "analysis_result")
    private String analysisResult;

    @Column
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;

    @Transient
    private String predictionDirectionFilter;

    @Transient
    private Double minimumConfidence;

    @Transient
    private Double maximumConfidence;
}
