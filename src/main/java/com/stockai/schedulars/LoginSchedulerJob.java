package com.stockai.schedulars;

import com.stockai.data.feed.AngelOneRestService;
import com.stockai.schedulars.job.BasicJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoginSchedulerJob extends BasicJob {

    @Autowired
    private MarketHistorySyncScheduler marketHistorySyncScheduler;

    @Autowired
    private AngelOneRestService angelOneRestService;

    @Autowired
    private PredictionComparisonScheduler predictionComparisonScheduler;

    @Autowired
    private PredictionSyncSchedular predictionSyncSchedular;

    @Scheduled(cron = "0 19 22 ? * MON-FRI")
    public void executeJob() {
        log.debug("Started login scheduler job");
        if (!canProcess()) {
            //Generate new session
            log.info("Generating new session for Angel One API");
            angelOneRestService.login();
            log.info("Login scheduler job executed successfully.");

            log.info("Starting market history sync job");
            marketHistorySyncScheduler.executeJob();
            log.info("Market history sync job executed successfully.");

            log.info("Starting prediction comparison scheduler after market history sync.");
            predictionComparisonScheduler.executeJob();
            log.info("Completed prediction comparison scheduler after market history sync.");

            log.info("Starting prediction sync scheduler after market history sync.");
            predictionSyncSchedular.executeJob();
            log.info("Completed prediction sync scheduler after market history sync.");
        }

        //logout the existing session
        angelOneRestService.logout();
        log.info("Logged out of existing Angel One session.");
        log.info("Ended login scheduler.");
    }
}

