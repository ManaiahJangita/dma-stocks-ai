package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionFiltersDto {
    private List<String> symbol;
    private List<String> nextDayHigh;
    private List<String> nextDayLow;
    private List<String> confidenceScore;
}
