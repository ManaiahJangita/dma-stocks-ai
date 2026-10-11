package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardFilterDto {
    private List<String> symbol;
    private List<String> nextDayHighDiff;
    private List<String> nextDayLowDiff;
    private List<String> confidenceScoreDiff;
}
