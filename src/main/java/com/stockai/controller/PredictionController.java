package com.stockai.controller;

import com.stockai.dto.PredictionRunResult;
import com.stockai.dto.PredictionRunRequest;
import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionView;
import com.stockai.service.interf.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {
    @Autowired
    private  PredictionService predictionService;

    @PostMapping("/runs")
    public ResponseEntity<?> startRun(@RequestBody(required = false) PredictionRunRequest request) {
        LocalDate endDate = request == null || request.getEndDate() == null ? LocalDate.now() : request.getEndDate();
        LocalDate startDate = request == null || request.getStartDate() == null
                ? endDate.minusYears(2)
            : request.getStartDate();
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().body("startDate must be on or before endDate");
        }
        return ResponseEntity.ok(predictionService.savePrediction(startDate.toString(), endDate.toString()));
    }

    @GetMapping("/runs")
    public List<PredictionRunResult> getRecentRuns(@ModelAttribute PredictionRunRequest filters) {
        return predictionService.getRecentRuns(filters);
    }

    @GetMapping("/runs/latest")
    public ResponseEntity<PredictionRunResult> getLatestRun() {
        return ResponseEntity.of(java.util.Optional.ofNullable(predictionService.getLatestRun()));
    }

    @GetMapping("/runs/{runId}/predictions")
    public ResponseEntity<?> getPredictions(
            @PathVariable UUID runId,
            @ModelAttribute PredictionDto filters) {
        if ((filters.getFromDate() != null && filters.getToDate() != null
                && filters.getFromDate().isAfter(filters.getToDate()))
                || (filters.getMinimumConfidence() != null && filters.getMaximumConfidence() != null
                && filters.getMinimumConfidence() > filters.getMaximumConfidence())) {
            return ResponseEntity.badRequest().body("Filter ranges must be in ascending order");
        }
        return ResponseEntity.ok(predictionService.getPredictions(runId, filters));
    }

    @GetMapping("/prediction-calculation")
    public ResponseEntity<PredictionRunResult> getPrediction() {
        LocalDate endDate = LocalDate.now();
        PredictionRunResult response = predictionService.savePrediction(
                endDate.minusYears(2).toString(), endDate.toString());
        return ResponseEntity.ok(response);
    }

}
