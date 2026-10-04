package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockAnalyzerDto {
    private UUID id;
    private String symbol;
    private Long predictionId;
    private UUID predictionRunId;
    private Double open;
    private Double high;
    private Double low;
    private Double latestClose;
    private Double previousClose;
    private Double tradingVolume;
    private Double per;
    private Double ptsC;
    private LocalDate historyDate;
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
    private String analysisResult;
    private String status;
    private LocalDateTime createdDate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String predictionDirectionFilter;
    private Double minimumConfidence;
    private Double maximumConfidence;
}
