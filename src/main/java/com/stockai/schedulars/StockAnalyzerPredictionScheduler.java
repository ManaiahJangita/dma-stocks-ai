package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.StockAnalyzerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StockAnalyzerPredictionScheduler extends BasicJob {
    private final StockAnalyzerService stockAnalyzerService;

    public StockAnalyzerPredictionScheduler(StockAnalyzerService stockAnalyzerService) {
        this.stockAnalyzerService = stockAnalyzerService;
    }

    @Override
    @Scheduled(cron = "0 45 17 ? * MON-FRI")
    public void executeJob() {
        if (canProcess()) {
            try {
                int synced = stockAnalyzerService.syncPredictionsToStockAnalyzer();
                log.info("Synchronized {} prediction rows into stock analyzer", synced);
            } catch (Exception exception) {
                log.error("Could not synchronize prediction rows into stock analyzer", exception);
            }
        }
    }
}