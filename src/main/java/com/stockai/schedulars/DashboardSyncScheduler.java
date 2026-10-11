package com.stockai.schedulars;

import com.stockai.service.interf.DashboardService;
import com.stockai.utils.StockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DashboardSyncScheduler {

    private final DashboardService dashboardService;

    public DashboardSyncScheduler(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    @Scheduled(cron = "0 30 18 ? * MON-FRI")
    public void executeJob() {
        if (!StockUtil.isTradingDay()) {
            return;
        }
        try {
            int rows = dashboardService.processDashboardData();
            log.info("Synchronized {} rows into dashboard", rows);
        } catch (Exception exception) {
            log.error("Could not synchronize dashboard data", exception);
        }
    }
}
