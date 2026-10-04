package com.stockai.repository;

import com.stockai.entity.StockAnalyzerEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAnalyzerRepository {
    List<StockAnalyzerEntity> findAllStockAnalyses(StockAnalyzerEntity filters);
    
    void upsertStockAnalyzerRows(List<StockAnalyzerEntity> rows);
}
