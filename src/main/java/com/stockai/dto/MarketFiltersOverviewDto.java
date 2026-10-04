package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketFiltersOverviewDto {
    private UUID id;
    private String market_summary;
    private String market_movers;
    private String market_status;
    private String market_signals;
    private String ai_predictions_summary;
    private String sector_performance;
    private String mywatchlist_summary;
}
