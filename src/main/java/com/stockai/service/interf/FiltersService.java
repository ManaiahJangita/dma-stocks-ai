package com.stockai.service.interf;

import com.stockai.dto.*;

import java.util.List;

public interface FiltersService {

    public ReportFilterDto getReportFilters();

    public PatternFilterDto getPatternFilters();

    public MarketFiltersDto getStockMarketFilters();

    public DashboardFilterDto getDashBoardFilters();

    public StockPredictionFiltersDto getStockPredictionFilters();
}
