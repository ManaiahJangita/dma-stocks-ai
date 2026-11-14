package com.stockai.controller;

import com.stockai.dto.HistoricalPredictionFiltersDto;
import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionFiltersDto;
import com.stockai.service.interf.PredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prediction")
@Slf4j
public class PredictionController {
    @Autowired
    private PredictionService predictionService;

    @PostMapping("/run-prediction")
    public ResponseEntity<List<PredictionDto>> getPredictionsByDateRange(@RequestBody PredictionFiltersDto predictionFilters) {
        log.info("Prediction request received with filters: {}", predictionFilters);
        try {
            List<PredictionDto> predictions = predictionService.getPredictionDetailsByFilters(predictionFilters);
            if (predictions == null || predictions.isEmpty()) {
                log.warn("Prediction request returned no data for filters: {}", predictionFilters);
                return ResponseEntity.noContent().build();
            }
            log.info("Prediction request completed successfully with {} records", predictions.size());
            return new ResponseEntity<>(predictions, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Prediction request failed for filters: {}", predictionFilters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/historical")
    public ResponseEntity<List<PredictionDto>> getHistoricalPredictionData(@RequestBody HistoricalPredictionFiltersDto filters) {
        log.info("Historical prediction POST request received with filters={}", filters);
        try {
            List<PredictionDto> response = predictionService.fetchHistoricalPredictionData(filters);
            if (response == null || response.isEmpty()) {
                log.warn("Historical prediction POST request returned no data for filters={}", filters);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Historical prediction POST request failed for filters={}", filters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
