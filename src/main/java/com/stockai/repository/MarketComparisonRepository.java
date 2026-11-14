package com.stockai.repository;

import com.stockai.entity.MarketComparisonEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketComparisonRepository {

    void saveComparison(List<MarketComparisonEntity> entities);

    void archiveComparison(List<MarketComparisonEntity> entities);

    void deleteByMarketDate();

    List<MarketComparisonEntity> findByMarketDate(LocalDate marketDate);

    List<MarketComparisonEntity> findBySymbolAndMarketDate(String symbol, LocalDate marketDate);
}

