package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.PredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class PredictionSyncSchedular extends BasicJob {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private DashboardSyncScheduler dashboardSyncScheduler;

    public void executeJob() {
        try {
            log.info("Starting prediction sync job");
            LocalDate startDate = LocalDate.now().minusYears(4);
            LocalDate endDate = LocalDate.now();
            predictionService.synchronizePredictions(startDate.toString(), endDate.toString());
            log.info("Completed prediction sync job");
            log.info("Starting dashboard sync job");
            dashboardSyncScheduler.executeJob();
            log.info("Completed dashboard sync job");
        } catch (Exception exception) {
            log.error("Could not synchronize prediction data", exception);
        }
    }
}
