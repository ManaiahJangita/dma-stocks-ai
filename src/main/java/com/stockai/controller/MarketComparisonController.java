package com.stockai.controller;

import com.stockai.dto.MarketComparisonDto;
import com.stockai.service.interf.MarketComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market-comparison")
public class MarketComparisonController {

    @Autowired
    private MarketComparisonService marketComparisonService;

    @GetMapping("/by-date")
    public ResponseEntity<List<MarketComparisonDto>> getByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate marketDate) {
        List<MarketComparisonDto> comparisons = marketComparisonService.getComparisonsByMarketDate(marketDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @GetMapping("/by-symbol")
    public ResponseEntity<List<MarketComparisonDto>> getBySymbolAndDate(
            @RequestParam String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate marketDate) {
        List<MarketComparisonDto> comparisons = marketComparisonService.getComparisonsBySymbolAndMarketDate(symbol, marketDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @GetMapping("/history/by-date")
    public ResponseEntity<List<MarketComparisonDto>> getHistoryByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate marketDate) {
        List<MarketComparisonDto> comparisons = marketComparisonService.getComparisonHistoryByMarketDate(marketDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @GetMapping("/history/by-symbol")
    public ResponseEntity<List<MarketComparisonDto>> getHistoryBySymbolAndDate(
            @RequestParam String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate marketDate) {
        List<MarketComparisonDto> comparisons = marketComparisonService.getComparisonHistoryBySymbolAndMarketDate(symbol, marketDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }
}
