package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.MarketComparisonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MarketComparisonScheduler extends BasicJob {

    private final MarketComparisonService marketComparisonService;

    public MarketComparisonScheduler(MarketComparisonService marketComparisonService) {
        this.marketComparisonService = marketComparisonService;
    }

    @Override
    public void executeJob() {
        log.info("Market comparison scheduler started");
        try {
            log.info("Comparing yesterday's market formula with today's actuals");
            marketComparisonService.compareYesterdayMarketFormulaWithTodaysActuals(null);
            log.info("Market comparison job completed successfully");
        } catch (Exception e) {
            log.error("Market comparison scheduler failed", e);
        }
    }
}

