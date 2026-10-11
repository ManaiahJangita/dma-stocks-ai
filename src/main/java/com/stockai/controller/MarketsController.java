package com.stockai.controller;

import com.stockai.dto.MarketDto;
import com.stockai.dto.MarketFiltersDto;
import com.stockai.service.interf.MarketsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedular/api/v1/markets")
public class MarketsController {
    @Autowired
    private MarketsService marketsService;

    @PostMapping("/markets-data")
    public ResponseEntity<List<MarketDto>> fetchMarketsData(@RequestBody MarketFiltersDto filters) {
        List<MarketDto> response = marketsService.fetchMarketsData(filters);
        if(response.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/markets")
    public ResponseEntity<List<MarketDto>> fetchMarketsData(@RequestParam(defaultValue = "0") Integer offset,
                                                           @RequestParam(defaultValue = "10000") Integer limit) {
        List<MarketDto> response = marketsService.fetchMarketsData(offset, limit);
        if(response.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Integer> refreshMarketFilterResults() {
        return ResponseEntity.ok(marketsService.syncTwoYearHistoricalData());
    }
}
