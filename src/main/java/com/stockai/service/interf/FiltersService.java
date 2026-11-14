package com.stockai.service.interf;

import com.stockai.dto.*;

public interface FiltersService {

    ReportFilterDto getReportFilters();

    PatternFilterDto getPatternFilters();

    MarketFiltersDto getStockMarketFilters();

    DashboardFilterDto getDashBoardFilters();

    StockPredictionFiltersDto getStockPredictionFilters();
}
