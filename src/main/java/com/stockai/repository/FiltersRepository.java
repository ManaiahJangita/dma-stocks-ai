package com.stockai.repository;

import com.stockai.entity.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiltersRepository {

    public List<String> getReportFilters();

    public List<String> getPatternFilters();

    public List<StockAnalyzerFiltersEntity> getStockAnalyzerFilters();

    public List<MarketFiltersEntity> getStockMarketFilters();

    public List<MarketOverviewFiltersEntity> getDashBoardFilters();
}
