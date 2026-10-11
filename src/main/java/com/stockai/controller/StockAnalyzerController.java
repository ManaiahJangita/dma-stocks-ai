package com.stockai.controller;

import com.stockai.dto.StockAnalyzerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-analyzer")
public class StockAnalyzerController {

    @GetMapping("/stock-analyzer-data")
    public ResponseEntity<List<StockAnalyzerDto>> fetchStockAnalyzerData(
            @ModelAttribute StockAnalyzerDto filters) {
        List<StockAnalyzerDto> response = new ArrayList<>();
        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
