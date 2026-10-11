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
public class MarketDto {
    private Integer id;

    private String name;
    private String status;

    private String symbol;

    private Double open;
    private Double high;
    private Double low;
    private Double close;

    private Long volume;
    private Double per;
    private Double ptsC;

    private LocalDateTime historicalCreatedDate;
    private LocalDate historicalUpdateDate;

    private LocalDate createdDate;
    private LocalDate updatedDate;

    private LocalDate marketDate;

    private Double previousClose;
    private Double changePercent;

    private Long avgVolume;

    private Double sma5;
    private Double sma10;
    private Double sma20;
    private Double sma50;

    private Double ema12;
    private Double ema26;

    private Double rsi14;
    private Double volumeRatio;
    private Double return1d;
    private Double return5d;
    private Double return10d;
    private Double return30d;
    private Double distanceFrom52wHighPct;
    private Double distanceFrom52wLowPct;
    private Double distanceFromSma20Pct;
    private Boolean breakoutFlag;
    private String trendLabel;
    private Double nextDayHigh;
    private Double nextDayReturnPct;
    private Double nextDayLow;
    private Double nextDayLowReturnPct;
    private Double confidenceScore;
}
