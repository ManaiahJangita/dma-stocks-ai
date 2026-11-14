package com.stockai.service.interf;

import com.stockai.dto.MarketDto;
import com.stockai.dto.MarketFiltersDto;
import com.stockai.dto.MarketHistoryFiltersDto;

import java.util.List;

public interface MarketsService {

    List<MarketDto> fetchMarketsData(MarketFiltersDto filters);

    List<MarketDto> fetchMarketsData(Integer offset, Integer limit);

    List<MarketDto> fetchHistoricalMarketData(MarketHistoryFiltersDto filters);

    int syncTwoYearHistoricalData();

}
