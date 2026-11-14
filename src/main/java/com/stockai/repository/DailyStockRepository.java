package com.stockai.repository;

import com.stockai.entity.DailyStockModel;
import com.stockai.entity.HistoricalDataEntity;

import java.util.List;

public interface DailyStockRepository {

     void saveAllDailyStockData(List<DailyStockModel> dailyStockModels);

     void saveAllHistoricalData(List<HistoricalDataEntity> dailyStockHistoryModels);

     int countAll();

     void truncateDailyStockData();

     List<DailyStockModel> fullData();

}
