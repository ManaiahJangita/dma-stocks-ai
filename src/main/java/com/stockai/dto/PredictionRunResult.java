package com.stockai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRunResult {
        private UUID runId;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime createdAt;
        private int predictionsSaved;
        private int skippedSymbols;
        private int validationSamples;
        private int correctValidationPredictions;
        private double validationAccuracyPercent;
        private String accuracyMetric;
}
