package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historical snapshot of predictions. Each row represents a prediction saved at a specific time.
 * Useful for auditing and analyzing how predictions change over time.
 * Mapped to dmastock.historical_prediction table.
 */
@Data
@Entity
@Table(name = "historical_prediction", schema = "dmastock")
public class HistoricalPredictionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "run_start_date")
    private LocalDate runStartDate;

    @Column(name = "run_end_date")
    private LocalDate runEndDate;

    @Column(name = "run_created_at")
    private LocalDateTime runCreatedAt;

    @Column(name = "predictions_saved")
    private Integer predictionsSaved;

    @Column(name = "skipped_symbols")
    private Integer skippedSymbols;

    @Column(name = "validation_samples")
    private Integer validationSamples;

    @Column(name = "correct_validation_predictions")
    private Integer correctValidationPredictions;

    @Column(name = "validation_accuracy_percent")
    private Double validationAccuracyPercent;

    @Column(name = "accuracy_metric", length = 160)
    private String accuracyMetric;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Double open;

    @Column(nullable = false)
    private Double high;

    @Column(nullable = false)
    private Double low;

    @Column(name = "close", nullable = false)
    private Double close;

    @Column(name = "volume", nullable = false)
    private Double volume;

    @Column(nullable = false)
    private Double per;

    @Column(nullable = false)
    private Double ptsC;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    @Column(name = "prediction_close", nullable = false)
    private Double predictionClose;

    @Column(name = "prediction_direction", nullable = false)
    private String predictionDirection;

    @Column(name = "prediction_close_percentage", nullable = false)
    private Double predictionClosePercentage;

    @Column(name = "predicted_high", nullable = false)
    private Double predictedHigh;

    @Column(name = "predicted_high_percentage", nullable = false)
    private Double predictedHighPercentage;

    @Column(name = "predicted_low", nullable = false)
    private Double predictedLow;

    @Column(name = "predicted_low_percentage", nullable = false)
    private Double predictedLowPercentage;

    @Column(name = "confidence_percent", nullable = false)
    private Double confidencePercent;

    @Column(name = "up_probability_percent", nullable = false)
    private Double upProbabilityPercent;

    @Column(name = "down_probability_percent", nullable = false)
    private Double downProbabilityPercent;

}
