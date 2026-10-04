package com.stockai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionView {
        private Long id;
        private UUID runId;
        private String symbol;
        private LocalDate historyDate;
        private LocalDate predictionDate;
        private double latestClose;
        private double predictedClose;
        private double predictedHigh;
        private double predictedLow;
        private String direction;
        private double confidencePercent;
        private double upProbabilityPercent;
        private double downProbabilityPercent;
}