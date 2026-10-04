package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import lombok.Data;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockAnalyzerFiltersDto {
    private UUID id;
    private String stock_search;
    private String stock_overview;
    private String price_analysis;
    private String Technical_analysis;
    private String volume_analysis;
    private String ai_prediction;
    private String historical_performance;
    private String risk_analysis;
    private String signals;
    private String news_events;
}
