package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionDto {
    private Long id;
    private UUID runId;
    private String symbol;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private double per;
    private double ptsC;
    private String createdDate;
    private double todayValue;
    private String mode;

    // Prediction properties
    private String predictionDate;
    private double predictionClose;
    private String predictionDirection;
    private double predictionClosePercentage;
    private double predictedHigh;
    private double predictedHighPercentage;
    private double predictedLow;
    private double predictedLowPercentage;
    private double increasePercentage;
    private double decreasePercentage;
    private double confidencePercent;
    private double upProbabilityPercent;
    private double downProbabilityPercent;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Double minimumConfidence;
    private Double maximumConfidence;
}
