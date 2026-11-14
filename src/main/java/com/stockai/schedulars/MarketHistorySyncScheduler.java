package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.MarketsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketHistorySyncScheduler extends BasicJob {

    private final MarketsService marketsService;
    @Autowired
    private MarketComparisonScheduler marketComparisonScheduler;

    public MarketHistorySyncScheduler(MarketsService marketsService) {
        this.marketsService = marketsService;
    }

    @Override
    public void executeJob() {
        try {
            log.info("Execute Job execution started for to load data for market.");
            int synchronizedRows = marketsService.syncTwoYearHistoricalData();
            log.info("Synchronized {} rows into market history", synchronizedRows);

            log.info("Starting market comparison job");
            marketComparisonScheduler.executeJob();
            log.info("Market comparison job executed successfully.");

        } catch (Exception exception) {
            log.error("Could not synchronize historical data into market history.", exception);
        }
    }
}