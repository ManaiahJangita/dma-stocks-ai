package com.stockai.controller;

import com.stockai.client.AngelOneService;
import com.stockai.schedulars.CustomersSyncSchedular;
import com.stockai.schedulars.PatternSyncScheduler;
import com.stockai.schedulars.PaymentPlanSyncScheduler;
import com.stockai.service.interf.DashboardService;
import com.stockai.service.interf.MarketsService;
import com.stockai.service.interf.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/schedular/historical-data")
public class DailyHistoricalController {

    @Autowired
    private AngelOneService angelOneService;

    @Autowired
    private MarketsService marketsService;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private  DashboardService dashboardService;

    @Autowired
    private PatternSyncScheduler patternSyncScheduler;

    @Autowired
    private PaymentPlanSyncScheduler paymentPlanSyncScheduler;

    @Autowired
    private CustomersSyncSchedular customersSyncSchedular;

    @GetMapping("/fetch-and-save")
    public ResponseEntity<String> fetchAndSaveHistoricalData() {
        try {
             angelOneService.generateSession();
            //marketsService.syncTwoYearHistoricalData();
            LocalDate startDate = LocalDate.now().minusYears(2);
            LocalDate endDate = LocalDate.now();
            //predictionService.synchronizePredictions(String.valueOf(startDate), String.valueOf(endDate));
            dashboardService.processDashboardData();
            patternSyncScheduler.executeJob();
            paymentPlanSyncScheduler.executeJob();
            customersSyncSchedular.executeJob();
            return ResponseEntity.ok("Historical data fetched and saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching and saving historical data: " + e.getMessage());
        }
    }
}
