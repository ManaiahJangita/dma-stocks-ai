package com.stockai.repository;

import com.stockai.entity.DailyStockModel;
import com.stockai.entity.HistoricalDataEntity;

import java.util.List;

public interface DailyStockRepository {

    public void saveAllDailyStockData(List<DailyStockModel> dailyStockModels);

    public void saveAllHistoricalData(List<HistoricalDataEntity> dailyStockHistoryModels);

    public int countAll();

    public void truncateDailyStockData();

    public List<DailyStockModel> fullData();

}
