package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoricalPredictionDto {
    private UUID id;
    private UUID runId;
    private LocalDate runStartDate;
    private LocalDate runEndDate;
    private LocalDateTime runCreatedAt;
    private Integer predictionsSaved;
    private Integer skippedSymbols;
    private Integer validationSamples;
    private Integer correctValidationPredictions;
    private Double validationAccuracyPercent;
    private String accuracyMetric;
    private String symbol;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Double per;
    private Double ptsC;
    private LocalDate createdDate;
    private LocalDate predictionDate;
    private Double predictionClose;
    private String predictionDirection;
    private Double predictionClosePercentage;
    private Double predictedHigh;
    private Double predictedHighPercentage;
    private Double predictedLow;
    private Double predictedLowPercentage;
    private Double confidencePercent;
    private Double upProbabilityPercent;
    private Double downProbabilityPercent;
}

