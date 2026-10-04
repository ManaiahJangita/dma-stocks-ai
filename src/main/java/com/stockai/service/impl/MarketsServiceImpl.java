package com.stockai.service.impl;

import com.stockai.dto.MarketDto;
import com.stockai.entity.HistoricalDataEntity;
import com.stockai.entity.MarketEntity;
import com.stockai.entity.PredictionEntity;
import com.stockai.entity.PredictionRunEntity;
import com.stockai.repository.HistoricalDataRepository;
import com.stockai.repository.MarketRepository;
import com.stockai.repository.PredictionRepository;
import com.stockai.service.interf.MarketsService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MarketsServiceImpl implements MarketsService {
    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private HistoricalDataRepository historicalDataRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Override
    public List<MarketDto> fetchMarketsData(MarketDto filters) {
        MarketDto request = filters == null ? new MarketDto() : filters;
        if ("PREDICTION".equalsIgnoreCase(request.getRecordType())) {
            return fetchLatestPredictions(request);
        }

        List<MarketDto> markets = new java.util.ArrayList<>();
        MarketEntity entityFilters = new MarketEntity();
        BeanUtils.copyProperties(request, entityFilters);
        for (MarketEntity entity : marketRepository.findAllMarketData(entityFilters)) {
            MarketDto dto = new MarketDto();
            BeanUtils.copyProperties(entity, dto);
            dto.setRecordType("MARKET");
            markets.add(dto);
        }
        return markets;
    }

    private List<MarketDto> fetchLatestPredictions(MarketDto filters) {
        PredictionRunEntity latestRun = predictionRepository.findLatestPredictionRun();
        if (latestRun == null) {
            return List.of();
        }

        PredictionEntity entityFilters = new PredictionEntity();
        entityFilters.setSymbol(filters.getSymbol() == null ? filters.getName() : filters.getSymbol());
        entityFilters.setFromDate(filters.getFromDate());
        entityFilters.setToDate(filters.getToDate());
        entityFilters.setPredictionDirectionFilter(filters.getPredictionDirectionFilter());
        entityFilters.setMinimumConfidence(filters.getMinimumConfidence());
        entityFilters.setMaximumConfidence(filters.getMaximumConfidence());

        List<MarketDto> predictions = new java.util.ArrayList<>();
        for (PredictionEntity entity : predictionRepository.findPredictions(latestRun.getId(), entityFilters)) {
            MarketDto dto = new MarketDto();
            dto.setName(entity.getSymbol());
            dto.setSymbol(entity.getSymbol());
            dto.setStatus("PREDICTION");
            dto.setRecordType("PREDICTION");
            dto.setPredictionId(entity.getId());
            dto.setPredictionRunId(entity.getRunId());
            dto.setOpen(entity.getOpen());
            dto.setHigh(entity.getHigh());
            dto.setLow(entity.getLow());
            dto.setClose(entity.getDay_end_close());
            dto.setPreviousClose(entity.getPrevious_close());
            dto.setVolume(entity.getTrd_vol() == null ? null : entity.getTrd_vol().longValue());
            dto.setPer(entity.getPer());
            dto.setPtsC(entity.getPtsC());
            dto.setMarketDate(entity.getCreated_date());
            dto.setCreatedDate(entity.getCreated_date().atStartOfDay());
            dto.setPredictionDate(LocalDate.parse(entity.getPrediction_date()));
            dto.setPredictionClose(entity.getPrediction_close());
            dto.setPredictionDirection(entity.getPrediction_direction());
            dto.setPredictionClosePercentage(entity.getPrediction_close_percentage());
            dto.setPredictedHigh(entity.getPredicted_high());
            dto.setPredictedHighPercentage(entity.getPredicted_high_percentage());
            dto.setPredictedLow(entity.getPredicted_low());
            dto.setPredictedLowPercentage(entity.getPredicted_low_percentage());
            dto.setConfidencePercent(entity.getConfidence_percent());
            dto.setUpProbabilityPercent(entity.getUp_probability_percent());
            dto.setDownProbabilityPercent(entity.getDown_probability_percent());
            predictions.add(dto);
        }
        return predictions;
    }

    @Override
    @Transactional
    public synchronized int syncOneYearHistoricalData() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        List<HistoricalDataEntity> history = historicalDataRepository.findAll(
                startDate.toString(), endDate.toString());
        Map<String, Double> previousCloseBySymbol = new HashMap<>();
        List<MarketEntity> marketRows = new java.util.ArrayList<>(history.size());

        for (HistoricalDataEntity historical : history) {
            MarketEntity market = new MarketEntity();
            market.setHistoricalDataId(historical.getId());
            market.setName(historical.getSymbol());
            market.setSymbol(historical.getSymbol());
            market.setStatus("HISTORICAL");
            market.setOpen(historical.getOpen());
            market.setHigh(historical.getHigh());
            market.setLow(historical.getLow());
            market.setClose(historical.getClose());
            market.setPreviousClose(previousCloseBySymbol.get(historical.getSymbol()));
            market.setVolume(historical.getVolume());
            market.setPer(historical.getPer());
            market.setPtsC(historical.getPtsC());
            market.setMarketDate(historical.getCreatedDate());
            market.setCreatedDate(historical.getCreatedDate().atStartOfDay());
            previousCloseBySymbol.put(historical.getSymbol(), historical.getClose());
            marketRows.add(market);
        }

        if (!marketRows.isEmpty()) {
            marketRepository.upsertHistoricalMarkets(marketRows);
        }
        marketRepository.deleteSyncedMarketHistoryBefore(startDate);
        log.info("Synchronized {} historical market rows from {} through {}",
                marketRows.size(), startDate, endDate);
        return marketRows.size();
    }
}
