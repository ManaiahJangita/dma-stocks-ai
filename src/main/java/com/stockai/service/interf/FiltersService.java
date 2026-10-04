package com.stockai.service.interf;

import com.stockai.dto.*;

import java.util.List;

public interface FiltersService {

    public ReportFilterDto getReportFilters();

    public PatternFilterDto getPatternFilters();

    public List<MarketFiltersDto> getStockMarketFilters();

    public List<MarketFiltersOverviewDto> getDashBoardFilters();

    public List<StockAnalyzerFiltersDto> getStockAnalyzerFilters();
}
