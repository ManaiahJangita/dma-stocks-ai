package com.stockai.controller;

import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionFiltersDto;
import com.stockai.dto.PredictionRunRequest;
import com.stockai.dto.PredictionRunResult;
import com.stockai.service.interf.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {
    @Autowired
    private PredictionService predictionService;

    @PostMapping("/run-prediction")
    public ResponseEntity<List<PredictionDto>> getPredictionsByDateRange(@RequestBody PredictionFiltersDto predictionFilters) {
        List<PredictionDto> predictions = predictionService.getPredictionDetailsByFilters(predictionFilters);
        if (predictions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
       return new ResponseEntity<>(predictions, HttpStatus.OK);
    }

}
