package com.stockai.service.interf;

import com.stockai.dto.MarketComparisonDto;

import java.time.LocalDate;
import java.util.List;

public interface MarketComparisonService {

    void compareYesterdayMarketFormulaWithTodaysActuals(LocalDate marketDate);

    List<MarketComparisonDto> getComparisonsByMarketDate(LocalDate marketDate);

    List<MarketComparisonDto> getComparisonsBySymbolAndMarketDate(String symbol, LocalDate marketDate);

    List<MarketComparisonDto> getComparisonHistoryByMarketDate(LocalDate marketDate);

    List<MarketComparisonDto> getComparisonHistoryBySymbolAndMarketDate(String symbol, LocalDate marketDate);

    void evaluateAndSendBuyAlertIfThresholdReached(LocalDate marketDate);
}

