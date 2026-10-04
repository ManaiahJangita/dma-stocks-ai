package com.stockai.apipridection.service;

import com.stockai.entity.HistoricalDataEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockAIServiceTest {
    private final StockAIService service = new StockAIService();

    @Test
    void trainsAndReportsChronologicalHoldoutMetrics() {
        StockAIService.AnalysisResult result = service.analyze(history(100));

        assertNotNull(result);
        assertEquals(16, result.getValidationSamples());
        assertTrue(result.getCorrectValidationPredictions() >= 0);
        assertTrue(result.getCorrectValidationPredictions() <= result.getValidationSamples());
        assertEquals(100.0, result.getUpProbabilityPercent() + result.getDownProbabilityPercent(), 1e-8);
        assertTrue(result.getPredictedHigh() >= result.getPredictedLow());
        assertTrue(result.getPredictedClose() > 0);
    }

    @Test
    void skipsHistoryTooShortForTrainingAndValidation() {
        assertNull(service.analyze(history(60)));
    }

    private static List<HistoricalDataEntity> history(int count) {
        List<HistoricalDataEntity> result = new ArrayList<>();
        double previousClose = 100.0;
        for (int index = 0; index < count; index++) {
            double close = 100.0 + index * 0.08 + Math.sin(index * 0.8) * 0.4;
            double open = index == 0 ? close : previousClose + Math.cos(index * 0.5) * 0.1;
            HistoricalDataEntity row = new HistoricalDataEntity();
            row.setSymbol("TEST");
            row.setOpen(open);
            row.setHigh(Math.max(open, close) + 0.4);
            row.setLow(Math.min(open, close) - 0.4);
            row.setClose(close);
            row.setVolume(1000 + index * 10L);
            row.setPer(10.0);
            row.setPtsC((close / previousClose - 1.0) * 100.0);
            row.setCreatedDate(LocalDate.of(2020, 1, 1).plusDays(index));
            result.add(row);
            previousClose = close;
        }
        return result;
    }
+}
