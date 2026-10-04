package com.stockai.service.interf;

import com.stockai.entity.DailyStockModel;

import java.util.List;

public interface DailyStockService {

    void processDailyStockData(List<DailyStockModel> models);

    int dailyStockServiceCount();

    void truncateDailyStockData();

    void insertHistoryTable();

    void fullData();
}
