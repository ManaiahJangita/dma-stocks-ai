package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionDto {
    private String symbol;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private LocalDate createdDate;
    private Double predictionClose;
    private String predictionDirection;
    private Double predictedHigh;
    private Double predictedHighPercentage;
    private Double predictedLow;
    private Double predictedLowPercentage;
    private Double confidencePercent;
    private Double upProbabilityPercent;
    private Double downProbabilityPercent;
}
