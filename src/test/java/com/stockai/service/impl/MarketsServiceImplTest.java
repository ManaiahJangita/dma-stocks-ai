package com.stockai.service.impl;

import com.stockai.dto.MarketDto;
import com.stockai.entity.PredictionEntity;
import com.stockai.entity.PredictionRunEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.MarketRepository;
import com.stockai.repository.PredictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketsServiceImplTest {
    @Mock
    private MarketRepository marketRepository;

    @Mock
    private HistoricalDataRepository historicalDataRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @InjectMocks
    private MarketsServiceImpl marketsService;

    @Test
    void returnsLatestPredictionRowsWhenRecordTypeIsPrediction() {
        UUID runId = UUID.randomUUID();
        PredictionRunEntity latestRun = new PredictionRunEntity();
        latestRun.setId(runId);
        when(predictionRepository.findLatestPredictionRun()).thenReturn(latestRun);

        PredictionEntity prediction = new PredictionEntity();
        prediction.setId(90L);
        prediction.setRunId(runId);
        prediction.setSymbol("TEST");
        prediction.setDay_end_close(100.0);
        prediction.setPrediction_close(102.0);
        prediction.setPrediction_direction("Up");
        prediction.setCreated_date(LocalDate.of(2026, 9, 30));
        prediction.setPrediction_date("2026-10-01");
        prediction.setConfidence_percent(68.0);
        when(predictionRepository.findPredictions(any(UUID.class), any(PredictionEntity.class)))
                .thenReturn(List.of(prediction));

        MarketDto filters = new MarketDto();
        filters.setRecordType("PREDICTION");
        filters.setSymbol("TEST");

        List<MarketDto> result = marketsService.fetchMarketsData(filters);

        verify(predictionRepository).findPredictions(any(UUID.class), any(PredictionEntity.class));
        assertEquals(1, result.size());
        assertEquals("PREDICTION", result.getFirst().getRecordType());
        assertEquals(90L, result.getFirst().getPredictionId());
        assertEquals(runId, result.getFirst().getPredictionRunId());
        assertEquals(102.0, result.getFirst().getPredictionClose());
        assertEquals("Up", result.getFirst().getPredictionDirection());
    }
}