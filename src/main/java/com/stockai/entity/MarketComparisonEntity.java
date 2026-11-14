package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "market_comparison", schema = "dmastock")
public class MarketComparisonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "market_run_id")
    private UUID marketRunId;

    @Column(name = "market_date", nullable = false)
    private LocalDate marketDate;

    @Column(name = "actual_date", nullable = false)
    private LocalDate actualDate;

    @Column(name = "predicted_open")
    private Double predictedOpen;

    @Column(name = "predicted_high")
    private Double predictedHigh;

    @Column(name = "predicted_low")
    private Double predictedLow;

    @Column(name = "predicted_close")
    private Double predictedClose;

    @Column(name = "predicted_direction", length = 20)
    private String predictedDirection;

    @Column(name = "actual_open")
    private Double actualOpen;

    @Column(name = "actual_high")
    private Double actualHigh;

    @Column(name = "actual_low")
    private Double actualLow;

    @Column(name = "actual_close")
    private Double actualClose;

    @Column(name = "actual_direction", length = 20)
    private String actualDirection;

    @Column(name = "close_diff")
    private Double closeDiff;

    @Column(name = "high_diff")
    private Double highDiff;

    @Column(name = "low_diff")
    private Double lowDiff;

    @Column(name = "close_diff_percent")
    private Double closeDiffPercent;

    @Column(name = "high_diff_percent")
    private Double highDiffPercent;

    @Column(name = "low_diff_percent")
    private Double lowDiffPercent;

    @Column(name = "direction_matched")
    private Boolean directionMatched;

    @Column(name = "status", length = 40)
    private String status;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "predicted_probability_up")
    private Double predictedProbabilityUp;

    @Column(name = "predicted_probability_down")
    private Double predictedProbabilityDown;

    @Column(name = "confidence_percent")
    private Double confidencePercent;

    @Column(name = "predicted_high_change_pct")
    private Double predictedHighChangePct;

    @Column(name = "predicted_low_change_pct")
    private Double predictedLowChangePct;

    @Column(name = "predicted_close_change_pct")
    private Double predictedCloseChangePct;

    @Column(name = "actual_vs_predicted_direction_match")
    private Boolean actualVsPredictedDirectionMatch;

    @Column(name = "model_validation_accuracy_percent")
    private Double modelValidationAccuracyPercent;

    @Column(name = "retraining_feedback_score")
    private Double retrainingFeedbackScore;
}

