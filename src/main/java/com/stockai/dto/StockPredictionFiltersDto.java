package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockPredictionFiltersDto {
    private List<String> symbol;
    private List<String> nextDayHigh;
    private List<String> nextDayLow;
    private List<String> confidenceScore;
}
