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
public class HistoricalPatternDto {
    private UUID id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdDate;
    private String filterValue;
    private String symbol;
    private Double metricValue;
    private Double changePercent;
    private Double closePrice;
    private Long volume;
    private Integer rankNo;
    private LocalDate dataDate;
    private String patternName;
    private String patternType;
    private String signalType;
    private Double volumeRatio;
    private Double sma20;
    private Double sma50;
    private Double rsi14;
    private String trendLabel;
    private Boolean breakoutFlag;
    private Double score;
    private String filterType;
}

