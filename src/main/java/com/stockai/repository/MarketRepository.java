package com.stockai.repository;

import com.stockai.dto.MarketFiltersDto;
import com.stockai.dto.MarketHistoryFiltersDto;
import com.stockai.entity.HistoricalMarketEntity;
import com.stockai.entity.MarketEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketRepository {
    List<MarketEntity> findAllMarketData(int offset, int limit);

    List<MarketEntity> findAllMarketData();

    List<HistoricalMarketEntity> findHistoricalMarketDataByFilters(MarketHistoryFiltersDto filters);

    void upsertHistoricalMarkets(List<MarketEntity> rows);

    void deleteHistoricalMarkets();

    void upsertHistoricalMarketsEveryday(List<HistoricalMarketEntity> rows);

    int getCount();

    List<MarketEntity> findMarketsByFilters(MarketFiltersDto filters);

}
