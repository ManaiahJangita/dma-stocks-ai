package com.stockai.repository;

import com.stockai.entity.MarketEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketRepository {
	List<MarketEntity> findAllMarketData(MarketEntity filters);

	void upsertHistoricalMarkets(List<MarketEntity> rows);

	int deleteSyncedMarketHistoryBefore(LocalDate cutoffDate);
}
