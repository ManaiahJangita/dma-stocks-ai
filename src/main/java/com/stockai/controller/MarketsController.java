package com.stockai.controller;

import com.stockai.dto.MarketDto;
import com.stockai.dto.MarketFiltersDto;
import com.stockai.dto.MarketHistoryFiltersDto;
import com.stockai.service.interf.MarketsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedular/api/v1/markets")
@Slf4j
public class MarketsController {
    @Autowired
    private MarketsService marketsService;

    @PostMapping("/markets-data")
    public ResponseEntity<List<MarketDto>> fetchMarketsData(@RequestBody MarketFiltersDto filters) {
        log.info("Markets filter request received with filters: {}", filters);
        try {
            List<MarketDto> response = marketsService.fetchMarketsData(filters);
            if (response == null || response.isEmpty()) {
                log.warn("Markets filter request returned no data for filters: {}", filters);
                return ResponseEntity.noContent().build();
            }
            log.info("Markets filter request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Markets filter request failed for filters: {}", filters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/markets")
    public ResponseEntity<List<MarketDto>> fetchMarketsData(@RequestParam(defaultValue = "0") Integer offset,
                                                            @RequestParam(defaultValue = "10000") Integer limit) {
        log.info("Markets pagination request received with offset={} and limit={}", offset, limit);
        try {
            List<MarketDto> response = marketsService.fetchMarketsData(offset, limit);
            if (response == null || response.isEmpty()) {
                log.warn("Markets pagination request returned no data for offset={} and limit={}", offset, limit);
                return ResponseEntity.noContent().build();
            }
            log.info("Markets pagination request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Markets pagination request failed for offset={} and limit={}", offset, limit, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Integer> refreshMarketFilterResults() {
        log.info("Market refresh request received");
        try {
            int refreshed = marketsService.syncTwoYearHistoricalData();
            log.info("Market refresh completed successfully with {} rows", refreshed);
            return ResponseEntity.ok(refreshed);
        } catch (Exception e) {
            log.error("Market refresh request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/historical")
    public ResponseEntity<List<MarketDto>> getHistoricalMarketData(@RequestBody MarketHistoryFiltersDto filters) {
        log.info("Historical market POST request received with filters={}", filters);
        try {
            List<MarketDto> response = marketsService.fetchHistoricalMarketData(filters);
            if (response == null || response.isEmpty()) {
                log.warn("Historical market POST request returned no data for filters={}", filters);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Historical market POST request failed for filters={}", filters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
