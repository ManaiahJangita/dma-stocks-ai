package com.stockai.service.interf;

import com.stockai.dto.MarketDto;

import java.util.List;

public interface MarketsService {

    List<MarketDto> fetchMarketsData(MarketDto filters);

    int syncOneYearHistoricalData();
}
