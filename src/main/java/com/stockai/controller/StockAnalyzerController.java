package com.stockai.controller;

import com.stockai.dto.StockAnalyzerDto;
import com.stockai.service.interf.StockAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-analyzer")
public class StockAnalyzerController {
    @Autowired
    private StockAnalyzerService stockAnalyzerService;

    @GetMapping("/stock-analyzer-data")
    public ResponseEntity<List<StockAnalyzerDto>> fetchStockAnalyzerData(
            @ModelAttribute StockAnalyzerDto filters) {
        List<StockAnalyzerDto> response = stockAnalyzerService.fetchStockAnalyzerData(filters);
        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/sync-from-predictions")
    public ResponseEntity<Integer> syncPredictionsToStockAnalyzer() {
        return ResponseEntity.ok(stockAnalyzerService.syncPredictionsToStockAnalyzer());
    }
}
