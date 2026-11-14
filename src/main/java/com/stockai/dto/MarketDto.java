package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketDto {
    private String symbol;

    private Double open;
    private Double high;
    private Double low;
    private Double close;

    private Long volume;

    private LocalDate createdDate;

    private LocalDate marketDate;

    private Double previousClose;
    private Double changePercent;
    private Long avgVolume;
    private Double return1d;
    private Boolean breakoutFlag;
    private String trendLabel;
    private Double nextDayHigh;
    private Double nextDayReturnPct;
    private Double nextDayLow;
    private Double nextDayLowReturnPct;
    private Double confidenceScore;
}
