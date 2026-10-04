package com.stockai.service.impl;

import com.stockai.dto.*;
import com.stockai.entity.CustomerEntity;
import com.stockai.entity.MarketFiltersEntity;
import com.stockai.entity.MarketOverviewFiltersEntity;
import com.stockai.entity.StockAnalyzerFiltersEntity;
import com.stockai.repository.FiltersRepository;
import com.stockai.service.interf.FiltersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FiltersServiceImpl implements FiltersService {

    @Autowired
    private FiltersRepository filtersRepository;

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
    public List<MarketFiltersDto> getStockMarketFilters() {
      List<MarketFiltersEntity> filters = filtersRepository.getStockMarketFilters();
        List<MarketFiltersDto> marketFilters = new ArrayList<>();
        for(MarketFiltersEntity entity : filters){
            MarketFiltersDto dto = new MarketFiltersDto();
            BeanUtils.copyProperties(entity,dto);
            marketFilters.add(dto);
        }
        return marketFilters;
    }
    @Override
    public List<MarketFiltersOverviewDto> getDashBoardFilters() {
        List<MarketOverviewFiltersEntity> filters = filtersRepository.getDashBoardFilters();
        List<MarketFiltersOverviewDto> marketFilters = new ArrayList<>();
        for(MarketOverviewFiltersEntity entity : filters){
            MarketFiltersOverviewDto dto = new MarketFiltersOverviewDto();
            BeanUtils.copyProperties(entity,dto);
            marketFilters.add(dto);
        }
        return marketFilters;
    }

    @Override
    public List<StockAnalyzerFiltersDto> getStockAnalyzerFilters() {
        List<StockAnalyzerFiltersEntity> filters = filtersRepository.getStockAnalyzerFilters();
        List<StockAnalyzerFiltersDto> marketFilters = new ArrayList<>();
        for(StockAnalyzerFiltersEntity entity : filters){
            StockAnalyzerFiltersDto dto = new StockAnalyzerFiltersDto();
            BeanUtils.copyProperties(entity,dto);
            marketFilters.add(dto);
        }
        return marketFilters;
    }
}
