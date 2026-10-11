package com.stockai.schedulars;

import com.stockai.dto.PredictionRunResult;
import com.stockai.service.interf.PredictionService;
import com.stockai.utils.StockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class PredictionSyncSchedular {

    @Autowired
    private PredictionService predictionService;

    /**
     * Run prediction sync daily at 5:30 PM on trading days (after market hours).
     * This generates predictions using the latest available historical data.
     */
    @Scheduled(cron = "0 30 17 ? * MON-FRI")
    public void executeJob() {
        if (!StockUtil.isTradingDay()) {
            log.info("Skipping prediction sync: Not a trading day");
            return;
        }
        try {
            log.info("Starting prediction sync job");
            LocalDate startDate = LocalDate.now().minusYears(2);
            LocalDate endDate = LocalDate.now();
            predictionService.synchronizePredictions(startDate.toString(), endDate.toString());
        } catch (Exception exception) {
            log.error("Could not synchronize prediction data", exception);
        }
    }
}
