package com.stockai.service.impl;

import com.stockai.entity.PredictionEntity;
import com.stockai.entity.StockAnalyzerEntity;
import com.stockai.repository.PredictionRepository;
import com.stockai.repository.StockAnalyzerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAnalyzerServiceImplTest {
    @Mock
    private StockAnalyzerRepository stockAnalyzerRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @InjectMocks
    private StockAnalyzerServiceImpl stockAnalyzerService;

    @Test
    void copiesPredictionFieldsIntoStockAnalyzerRows() {
        UUID runId = UUID.randomUUID();
        PredictionEntity prediction = new PredictionEntity();
        prediction.setId(55L);
        prediction.setRunId(runId);
        prediction.setSymbol("TEST");
        prediction.setOpen(100.0);
        prediction.setHigh(105.0);
        prediction.setLow(98.0);
        prediction.setDay_end_close(102.0);
        prediction.setPrevious_close(99.0);
        prediction.setTrd_vol(1200.0);
        prediction.setPer(3.0);
        prediction.setPtsC(3.0);
        prediction.setCreated_date(LocalDate.of(2026, 9, 30));
        prediction.setPrediction_date("2026-10-01");
        prediction.setPrediction_close(103.0);
        prediction.setPrediction_direction("Up");
        prediction.setPrediction_close_percentage(0.98);
        prediction.setPredicted_high(106.0);
        prediction.setPredicted_high_percentage(3.92);
        prediction.setPredicted_low(100.0);
        prediction.setPredicted_low_percentage(-1.96);
        prediction.setConfidence_percent(72.0);
        prediction.setUp_probability_percent(72.0);
        prediction.setDown_probability_percent(28.0);
        when(predictionRepository.findAllPredictions()).thenReturn(List.of(prediction));

        int synchronizedRows = stockAnalyzerService.syncPredictionsToStockAnalyzer();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockAnalyzerEntity>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockAnalyzerRepository).upsertStockAnalyzerRows(rowsCaptor.capture());
        StockAnalyzerEntity row = rowsCaptor.getValue().getFirst();
        assertEquals(1, synchronizedRows);
        assertEquals(55L, row.getPredictionId());
        assertEquals(runId, row.getPredictionRunId());
        assertEquals("TEST", row.getSymbol());
        assertEquals(102.0, row.getLatestClose());
        assertEquals(103.0, row.getPredictionClose());
        assertEquals("Up", row.getPredictionDirection());
        assertEquals(72.0, row.getConfidencePercent());
        assertEquals(LocalDate.of(2026, 9, 30), row.getHistoryDate());
        assertEquals(LocalDate.of(2026, 10, 1), row.getPredictionDate());
    }
}