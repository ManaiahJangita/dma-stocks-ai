package com.stockai.service.impl;

import com.stockai.apiconstants.ApiConstants;
import com.stockai.dto.*;
import com.stockai.repository.FiltersRepository;
import com.stockai.repository.InstrumentsRepository;
import com.stockai.service.interf.FiltersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FiltersServiceImpl implements FiltersService {

    @Autowired
    private FiltersRepository filtersRepository;

    @Autowired
    private InstrumentsRepository instrumentsRepository;

    private static List<String> symbols;

    @Override
    public ReportFilterDto getReportFilters() {
        List<String> filters = filtersRepository.getReportFilters();
            ReportFilterDto reportFilterDto = new ReportFilterDto();
            reportFilterDto.setReports(filters);
        return reportFilterDto;
    }

    @Override
    public PatternFilterDto getPatternFilters() {
        List<String> filters = filtersRepository.getPatternFilters();
        PatternFilterDto patternFilters =  new PatternFilterDto();
        patternFilters.setPatterns(filters);
        return patternFilters;
    }

    @Override
    public MarketFiltersDto getStockMarketFilters() {
        if(symbols == null) {
            symbols = instrumentsRepository.findInstrumentsSymbols().stream().distinct().toList();
        }
      MarketFiltersDto marketFilters = new MarketFiltersDto();
        marketFilters.setSymbol(symbols);
        marketFilters.setNextDayHigh(ApiConstants.nextDayHigh);
        marketFilters.setNextDayLow(ApiConstants.nextDayLow);
        marketFilters.setConfidenceScore(ApiConstants.confidence);
        return marketFilters;
    }
    @Override
    public DashboardFilterDto getDashBoardFilters() {
        if(symbols == null) {
            symbols = instrumentsRepository.findInstrumentsSymbols().stream().distinct().toList();
        }
        DashboardFilterDto dashboardFilterDto = new DashboardFilterDto();
        dashboardFilterDto.setSymbol(symbols);
        dashboardFilterDto.setNextDayHighDiff(ApiConstants.nextDayHigh);;
        dashboardFilterDto.setNextDayLowDiff(ApiConstants.nextDayLow);
        dashboardFilterDto.setConfidenceScoreDiff(ApiConstants.confidence);
        return dashboardFilterDto;
    }

    @Override
    public StockPredictionFiltersDto getStockPredictionFilters() {
        if(symbols == null) {
            symbols = instrumentsRepository.findInstrumentsSymbols().stream().distinct().toList();
        }
        StockPredictionFiltersDto stockPredictionFilters = new StockPredictionFiltersDto();
        stockPredictionFilters.setSymbol(symbols);
        stockPredictionFilters.setNextDayHigh(ApiConstants.nextDayHigh);
        stockPredictionFilters.setNextDayLow(ApiConstants.nextDayLow);
        stockPredictionFilters.setConfidenceScore(ApiConstants.confidence);
        return stockPredictionFilters;
    }
}
