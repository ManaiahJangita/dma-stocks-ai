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
        log.info("Fetching report filters");
        try {
            List<String> filters = filtersRepository.getReportFilters();
            ReportFilterDto reportFilterDto = new ReportFilterDto();
            reportFilterDto.setReports(filters);
            log.info("Fetched {} report filter values", filters == null ? 0 : filters.size());
            return reportFilterDto;
        } catch (Exception e) {
            log.error("Failed to fetch report filters", e);
            throw e;
        }
    }

    @Override
    public PatternFilterDto getPatternFilters() {
        log.info("Fetching pattern filters");
        try {
            List<String> filters = filtersRepository.getPatternFilters();
            PatternFilterDto patternFilters = new PatternFilterDto();
            patternFilters.setPatterns(filters);
            log.info("Fetched {} pattern filter values", filters == null ? 0 : filters.size());
            return patternFilters;
        } catch (Exception e) {
            log.error("Failed to fetch pattern filters", e);
            throw e;
        }
    }

    @Override
    public MarketFiltersDto getStockMarketFilters() {
        log.info("Fetching stock market filters");
        try {
            if (symbols == null) {
                symbols = instrumentsRepository.findInstrumentsSymbols().stream().distinct().toList();
                log.info("Loaded {} stock symbols for market filters", symbols.size());
            }
            MarketFiltersDto marketFilters = new MarketFiltersDto();
            marketFilters.setSymbol(symbols);
            marketFilters.setNextDayHigh(ApiConstants.nextDayHigh);
            marketFilters.setNextDayLow(ApiConstants.nextDayLow);
            marketFilters.setConfidenceScore(ApiConstants.confidence);
            log.info("Stock market filter DTO prepared successfully");
            return marketFilters;
        } catch (Exception e) {
            log.error("Failed to prepare stock market filters", e);
            throw e;
        }
    }

    @Override
    public DashboardFilterDto getDashBoardFilters() {
        log.info("Fetching dashboard filters");
        try {
            if (symbols == null) {
                symbols = instrumentsRepository.findInstrumentsSymbols().stream().distinct().toList();
                log.info("Loaded {} stock symbols for dashboard filters", symbols.size());
            }
            DashboardFilterDto dashboardFilterDto = new DashboardFilterDto();
            dashboardFilterDto.setSymbol(symbols);
            dashboardFilterDto.setNextDayHighDiff(ApiConstants.nextDayHigh);
            ;
            dashboardFilterDto.setNextDayLowDiff(ApiConstants.nextDayLow);
            dashboardFilterDto.setConfidenceScoreDiff(ApiConstants.confidence);
            log.info("Dashboard filter DTO prepared successfully");
            return dashboardFilterDto;
        } catch (Exception e) {
            log.error("Failed to prepare dashboard filters", e);
            throw e;
        }
    }

    @Override
    public StockPredictionFiltersDto getStockPredictionFilters() {
        log.info("Fetching stock prediction filters");
        try {
            if (symbols == null) {
                symbols = instrumentsRepository.findInstrumentsSymbols().stream().distinct().toList();
                log.info("Loaded {} stock symbols for prediction filters", symbols.size());
            }
            StockPredictionFiltersDto stockPredictionFilters = new StockPredictionFiltersDto();
            stockPredictionFilters.setSymbol(symbols);
            stockPredictionFilters.setNextDayHigh(ApiConstants.nextDayHigh);
            stockPredictionFilters.setNextDayLow(ApiConstants.nextDayLow);
            stockPredictionFilters.setConfidenceScore(ApiConstants.confidence);
            log.info("Prediction filter DTO prepared successfully");
            return stockPredictionFilters;
        } catch (Exception e) {
            log.error("Failed to prepare stock prediction filters", e);
            throw e;
        }
    }
}
