package com.stockai.controller;

import com.stockai.dto.*;
import com.stockai.service.interf.FiltersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/filter")
@Slf4j
public class FliterApiContrller {

    @Autowired
    private FiltersService filtersService;

    @GetMapping("/reports-filters")
    public ResponseEntity<ReportFilterDto> getReportFilters() {
        log.info("Request received for report filters");
        try {
            ReportFilterDto response = filtersService.getReportFilters();
            if (response == null) {
                log.warn("Report filters are not available");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            log.info("Report filters loaded successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch report filters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/patterns-filters")
    public ResponseEntity<PatternFilterDto> getPatternFilters() {
        log.info("Request received for pattern filters");
        try {
            PatternFilterDto response = filtersService.getPatternFilters();
            if (response == null) {
                log.warn("Pattern filters are not available");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            log.info("Pattern filters loaded successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch pattern filters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stock-market-filters")
    public ResponseEntity<MarketFiltersDto> getStockMarketFilters() {
        log.info("Request received for stock market filters");
        try {
            MarketFiltersDto response = filtersService.getStockMarketFilters();
            if (response == null) {
                log.warn("Stock market filters are not available");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            log.info("Stock market filters loaded successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch stock market filters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stock-analyzer-filters")
    public ResponseEntity<StockPredictionFiltersDto> getStockAnalyzerFilters() {
        log.info("Request received for stock analyzer filters");
        try {
            StockPredictionFiltersDto response = filtersService.getStockPredictionFilters();
            if (response == null) {
                log.warn("Stock analyzer filters are not available");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            log.info("Stock analyzer filters loaded successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch stock analyzer filters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GetMapping("/dashboard-filters")
    public ResponseEntity<DashboardFilterDto> getDashBoardFilters() {
        log.info("Request received for dashboard filters");
        try {
            DashboardFilterDto response = filtersService.getDashBoardFilters();
            if (response == null) {
                log.warn("Dashboard filters are not available");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            log.info("Dashboard filters loaded successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch dashboard filters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
