package com.stockai.service.impl;

import com.stockai.dto.StockAnalyzerDto;
import com.stockai.entity.StockAnalyzerEntity;
import com.stockai.repository.StockAnalyzerRepository;
import com.stockai.service.interf.StockAnalyzerService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.stockai.entity.PredictionEntity;
import com.stockai.repository.PredictionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StockAnalyzerServiceImpl implements StockAnalyzerService {

    @Autowired
    private StockAnalyzerRepository stockAnalyzerRepository;
    @Autowired
    private PredictionRepository predictionRepository;

    @Override
    public List<StockAnalyzerDto> fetchStockAnalyzerData(StockAnalyzerDto filters) {
        List<StockAnalyzerDto> analyses = new ArrayList<>();
        StockAnalyzerEntity entityFilters = new StockAnalyzerEntity();
        BeanUtils.copyProperties(filters, entityFilters);
        for (StockAnalyzerEntity entity : stockAnalyzerRepository.findAllStockAnalyses(entityFilters)) {
            StockAnalyzerDto dto = new StockAnalyzerDto();
            BeanUtils.copyProperties(entity, dto);
            analyses.add(dto);
        }
        return analyses;
    }

    @Override
    @Transactional
    public synchronized int syncPredictionsToStockAnalyzer() {
        List<PredictionEntity> predictions = predictionRepository.findAllPredictions();
        List<StockAnalyzerEntity> rows = new ArrayList<>(predictions.size());
        for (PredictionEntity prediction : predictions) {
            StockAnalyzerEntity row = new StockAnalyzerEntity();
            row.setPredictionId(prediction.getId());
            row.setPredictionRunId(prediction.getRunId());
            row.setSymbol(prediction.getSymbol());
            row.setOpen(prediction.getOpen());
            row.setHigh(prediction.getHigh());
            row.setLow(prediction.getLow());
            row.setLatestClose(prediction.getDay_end_close());
            row.setPreviousClose(prediction.getPrevious_close());
            row.setTradingVolume(prediction.getTrd_vol());
            row.setPer(prediction.getPer());
            row.setPtsC(prediction.getPtsC());
            row.setHistoryDate(prediction.getCreated_date());
            row.setPredictionDate(parseDate(prediction.getPrediction_date()));
            row.setPredictionClose(prediction.getPrediction_close());
            row.setPredictionDirection(prediction.getPrediction_direction());
            row.setPredictionClosePercentage(prediction.getPrediction_close_percentage());
            row.setPredictedHigh(prediction.getPredicted_high());
            row.setPredictedHighPercentage(prediction.getPredicted_high_percentage());
            row.setPredictedLow(prediction.getPredicted_low());
            row.setPredictedLowPercentage(prediction.getPredicted_low_percentage());
            row.setConfidencePercent(prediction.getConfidence_percent());
            row.setUpProbabilityPercent(prediction.getUp_probability_percent());
            row.setDownProbabilityPercent(prediction.getDown_probability_percent());
            row.setAnalysisResult(prediction.getPrediction_direction() + " forecast; close "
                    + prediction.getPrediction_close());
            row.setStatus("PREDICTED");
            row.setCreatedDate(prediction.getCreated_date().atStartOfDay());
            rows.add(row);
        }
        if (!rows.isEmpty()) {
            stockAnalyzerRepository.upsertStockAnalyzerRows(rows);
        }
        return rows.size();
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
