package com.stockai.controller;

import com.stockai.dto.*;
import com.stockai.entity.CustomerEntity;
import com.stockai.service.interf.FiltersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/filter")
public class FliterApiContrller {

    @Autowired
    private FiltersService filtersService;

    @GetMapping("/reports-filters")
    public ResponseEntity<ReportFilterDto> getReportFilters(){

        ReportFilterDto response=  filtersService.getReportFilters();
        if(response==null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/patterns-filters")
    public ResponseEntity<PatternFilterDto> getPatternFilters(){
        PatternFilterDto response = filtersService.getPatternFilters();
        if(response==null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<> (response, HttpStatus.OK);
    }

    @GetMapping("/stock-market-filters")
    public ResponseEntity<MarketFiltersDto> getStockMarketFilters(){
        MarketFiltersDto response = filtersService.getStockMarketFilters();
        if(response==null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/stock-analyzer-filters")
    public ResponseEntity<StockPredictionFiltersDto> getStockAnalyzerFilters(){
        StockPredictionFiltersDto response = filtersService.getStockPredictionFilters();
        if(response==null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping("/dashboard-filters")
    public ResponseEntity<DashboardFilterDto> getDashBoardFilters(){
        DashboardFilterDto response = filtersService.getDashBoardFilters();
        if(response==null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
