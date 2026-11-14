package com.stockai.controller;

import com.stockai.dto.PredictionComparisonDto;
import com.stockai.service.interf.EmailDeliveryService;
import com.stockai.service.interf.PredictionComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/prediction-comparison")
public class PredictionComparisonController {

    @Autowired
    private PredictionComparisonService predictionComparisonService;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @GetMapping("/test-alert-email")
    public ResponseEntity<String> sendSampleAlertEmail(@RequestParam String email) {
        try {
            String subject = "Sample Stock Buy Alert - Test Email";
            String body = "This is a sample prediction alert email from Stock AI.\n\n"
                    + "Model status: TEST MODE\n"
                    + "Accuracy: 95.00%\n"
                    + "Matched predictions: 95 / 100\n\n"
                    + "This means the model is currently strong enough to consider buying stocks for the next trading day based on the same model logic.\n\n"
                    + "Important: This is a sample alert, not financial advice.\n"
                    + "Please validate with your own risk management and market conditions before placing trades.";

            emailDeliveryService.sendPredictionAccuracyAlert(email, subject, body);
            return ResponseEntity.ok("Sample prediction alert email sent to " + email);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send sample alert email: " + e.getMessage());
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<PredictionComparisonDto>> getByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate predictionDate) {
        List<PredictionComparisonDto> comparisons = predictionComparisonService.getComparisonsByPredictionDate(predictionDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @GetMapping("/by-symbol")
    public ResponseEntity<List<PredictionComparisonDto>> getBySymbolAndDate(
            @RequestParam String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate predictionDate) {
        List<PredictionComparisonDto> comparisons = predictionComparisonService.getComparisonsBySymbolAndPredictionDate(symbol, predictionDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @GetMapping("/history/by-date")
    public ResponseEntity<List<PredictionComparisonDto>> getHistoryByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate predictionDate) {
        List<PredictionComparisonDto> comparisons = predictionComparisonService.getComparisonHistoryByPredictionDate(predictionDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @GetMapping("/history/by-symbol")
    public ResponseEntity<List<PredictionComparisonDto>> getHistoryBySymbolAndDate(
            @RequestParam String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate predictionDate) {
        List<PredictionComparisonDto> comparisons = predictionComparisonService.getComparisonHistoryBySymbolAndPredictionDate(symbol, predictionDate);
        if (comparisons == null || comparisons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comparisons);
    }

    @PostMapping("/compare")
    public ResponseEntity<String> triggerComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate predictionDate) {
        try {
            predictionComparisonService.compareYesterdayPredictionsWithTodaysActuals(predictionDate);
            return ResponseEntity.ok("Prediction comparison executed successfully for " + (predictionDate != null ? predictionDate : "latest available date"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to compare prediction data: " + e.getMessage());
        }
    }
}
