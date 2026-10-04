package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketFiltersDto {
    private UUID id;
    private String market_indices;
    private String market_performance;
    private String price_movement;
    private String volume_delivery;
    private String technical_signals;
    private String breakouts;
    private String ai_prediction;
    private String market_sentiment;
    private String sector_performance;
    private String time_period;

}
