package com.stockai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
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

        public PredictionRunResult(UUID runId,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  LocalDateTime createdAt,
                                  int predictionsSaved,
                                  int skippedSymbols,
                                  int validationSamples,
                                  int correctValidationPredictions,
                                  double validationAccuracyPercent,
                                  String accuracyMetric) {
                this.runId = runId;
                this.startDate = startDate;
                this.endDate = endDate;
                this.createdAt = createdAt;
                this.predictionsSaved = predictionsSaved;
                this.skippedSymbols = skippedSymbols;
                this.validationSamples = validationSamples;
                this.correctValidationPredictions = correctValidationPredictions;
                this.validationAccuracyPercent = validationAccuracyPercent;
                this.accuracyMetric = accuracyMetric;
        }

        public PredictionRunResult(UUID runId,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  LocalDateTime createdAt,
                                  int predictionsSaved,
                                  int skippedSymbols,
                                  int validationSamples,
                                  int correctValidationPredictions,
                                  double validationAccuracyPercent,
                                  String accuracyMetric,
                                  String status) {
                this(runId, startDate, endDate, createdAt, predictionsSaved, skippedSymbols,
                        validationSamples, correctValidationPredictions, validationAccuracyPercent, accuracyMetric);
        }
}
