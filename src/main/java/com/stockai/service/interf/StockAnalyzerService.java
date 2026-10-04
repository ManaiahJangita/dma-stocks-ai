package com.stockai.service.interf;

import com.stockai.dto.StockAnalyzerDto;

import java.util.List;

public interface StockAnalyzerService {

    List<StockAnalyzerDto> fetchStockAnalyzerData(StockAnalyzerDto filters);

    int syncPredictionsToStockAnalyzer();
}
