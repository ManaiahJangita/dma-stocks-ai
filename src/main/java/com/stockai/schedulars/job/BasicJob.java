package com.stockai.schedulars.job;

import com.stockai.utils.StockUtil;

public abstract class BasicJob {

    public boolean canProcess() {
        return StockUtil.isTradingDay() && checkIntradayStartTime() && checkIntradayEndTime();
    }

    public boolean checkIntradayEndTime() {
        return StockUtil.checkIntradayEndTime();
    }

    public boolean checkIntradayStartTime() {
        return StockUtil.checkIntradayStartTime();
    }

    public abstract void executeJob();
}
