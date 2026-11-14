package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.PredictionComparisonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class PredictionComparisonScheduler extends BasicJob {

    @Autowired
    private PredictionComparisonService predictionComparisonService;

    @Override
    public void executeJob() {
        comparePredictionVsActuals();
    }

    public void comparePredictionVsActuals() {
        try {
            LocalDate today = LocalDate.now();
            log.info("Starting prediction vs actual comparison using latest available data as of {}", today);
            predictionComparisonService.compareYesterdayPredictionsWithTodaysActuals(null);
            log.info("Completed prediction vs actual comparison for {}", today);
        } catch (Exception e) {
            log.error("Prediction comparison scheduler failed", e);
        }
    }
}
