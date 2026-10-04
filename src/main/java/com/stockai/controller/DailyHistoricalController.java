package com.stockai.controller;

import com.stockai.client.AngelOneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/schedular/historical-data")
public class DailyHistoricalController {

    @Autowired
    private AngelOneService angelOneService;

    @GetMapping("/fetch-and-save")
    public ResponseEntity<String> fetchAndSaveHistoricalData() {
        try {
            angelOneService.generateSession();
            return ResponseEntity.ok("Historical data fetched and saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching and saving historical data: " + e.getMessage());
        }
    }
}
