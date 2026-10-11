package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.MarketsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Component
public class MarketHistorySyncScheduler extends BasicJob {

    private final MarketsService marketsService;

    public MarketHistorySyncScheduler(MarketsService marketsService) {
        this.marketsService = marketsService;
    }

    @Override
    @Scheduled(cron = "0 0 17 ? * MON-FRI")
    public void executeJob() {
        if (canProcess()) {
            try {
                log.info("Execute Job execution started for to load 2year data for market.");
                int synchronizedRows = marketsService.syncTwoYearHistoricalData();
                log.info("Synchronized {} rows into market history", synchronizedRows);
            } catch (Exception exception) {
                log.error("Could not synchronize historical data into market history.", exception);
            }
        }
    }
}