package com.stockai.service.impl;

import com.stockai.apipridection.service.StockAIService;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.PredictionEntity;
import com.stockai.entity.PredictionRunEntity;
import com.stockai.repository.PredictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceImplTest {
    @Mock
    private HistoricalDataService historicalDataService;

    @Mock
    private StockAIService stockAIService;

    @Mock
    private PredictionRepository predictionRepository;

    @InjectMocks
    private PredictionServiceImpl predictionService;

    @Test
    void savesForecastsAndMetricsAsOneRun() {
        UUID runId = UUID.randomUUID();
        LocalDate historyDate = LocalDate.of(2025, 8, 20);
        HistoricalDataEntity history = historicalRow(historyDate);
        when(historicalDataService.getAllHistoricalData("2024-08-20", "2026-08-20"))
                .thenReturn(List.of(history));
        when(stockAIService.analyze(List.of(history))).thenReturn(new StockAIService.AnalysisResult(
                112.0, 97.0, 105.0, 12.0, -3.0, 5.0, "Up", 70.0, 70.0, 30.0, 10, 7));
        doAnswer(invocation -> {
            PredictionRunEntity run = invocation.getArgument(0);
            run.setId(runId);
            return null;
        }).when(predictionRepository).savePredictionRun(any(PredictionRunEntity.class));

        var result = predictionService.savePrediction("2024-08-20", "2026-08-20");

        assertEquals(runId, result.getRunId());
        assertEquals(1, result.getPredictionsSaved());
        assertEquals(10, result.getValidationSamples());
        assertEquals(70.0, result.getValidationAccuracyPercent());
        verify(predictionRepository).savePrediction(ArgumentMatchers.argThat(rows ->
                rows.size() == 1 && rows.get(0).getRunId().equals(runId)));
    }

    @Test
    void rejectsReversedDateWindowsBeforeQueryingHistory() {
        assertThrows(IllegalArgumentException.class,
                () -> predictionService.savePrediction("2026-08-20", "2024-08-20"));

        verify(historicalDataService, never()).getAllHistoricalData(any(), any());
        verify(predictionRepository, never()).savePredictionRun(any());
    }

    private static HistoricalDataEntity historicalRow(LocalDate date) {
        HistoricalDataEntity row = new HistoricalDataEntity();
        row.setSymbol("TEST");
        row.setOpen(100.0);
        row.setHigh(102.0);
        row.setLow(99.0);
        row.setClose(101.0);
        row.setVolume(1200L);
        row.setPer(12.0);
        row.setPtsC(1.0);
        row.setCreatedDate(date);
        return row;
    }
}