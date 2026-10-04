package com.stockai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "prediction_run", schema = "dmastock")
public class PredictionRunEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "predictions_saved", nullable = false)
    private int predictionsSaved;

    @Column(name = "skipped_symbols", nullable = false)
    private int skippedSymbols;

    @Column(name = "validation_samples", nullable = false)
    private int validationSamples;

    @Column(name = "correct_validation_predictions", nullable = false)
    private int correctValidationPredictions;

    @Column(name = "validation_accuracy_percent", nullable = false)
    private double validationAccuracyPercent;

    @Column(name = "accuracy_metric", nullable = false, length = 160)
    private String accuracyMetric;
    
    @jakarta.persistence.Transient
    private Integer limit;
}