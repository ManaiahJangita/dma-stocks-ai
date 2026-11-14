package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DashboardSyncScheduler extends BasicJob {

    private final DashboardService dashboardService;

    @Autowired
    private PatternSyncScheduler patternSyncScheduler;

    public DashboardSyncScheduler(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public void executeJob() {
        try {

            log.info("Starting dashboard sync job");
            int rows = dashboardService.processDashboardData();
            log.info("Synchronized {} rows into dashboard", rows);

            log.info("Starting pattern sync job");
            patternSyncScheduler.executeJob();
            log.info("Completed pattern sync job");

        } catch (Exception exception) {
            log.error("Could not synchronize dashboard data", exception);
        }
    }
}
