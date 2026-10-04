package com.stockai.controller;

import com.stockai.dto.MarketDto;
import com.stockai.service.interf.MarketsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markets")
public class MarketsController {
    @Autowired
    private MarketsService marketsService;

    @GetMapping("/markets-data")
    public ResponseEntity<List<MarketDto>> fetchMarketsData(@ModelAttribute MarketDto filters) {
        List<MarketDto> response = marketsService.fetchMarketsData(filters);

        if(response.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/sync-one-year")
    public ResponseEntity<Integer> syncOneYearHistoricalData() {
        return ResponseEntity.ok(marketsService.syncOneYearHistoricalData());
    }
}
