package com.stockai.service.interf;

import com.stockai.dto.MarketDto;
import com.stockai.dto.MarketFiltersDto;

import java.util.List;

public interface MarketsService {

    List<MarketDto> fetchMarketsData(MarketFiltersDto filters);

    List<MarketDto> fetchMarketsData(Integer offset, Integer limit);

    int syncTwoYearHistoricalData();

}
