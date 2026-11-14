package com.stockai.controller;

import com.stockai.dto.DashboardDto;
import com.stockai.dto.DashboardFilterDto;
import com.stockai.dto.HistoricalDashboardFiltersDto;
import com.stockai.entity.HistoricalDashboardEntity;
import com.stockai.service.interf.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Slf4j
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @PostMapping("/dashboard-data")
    public ResponseEntity<List<DashboardDto>> getDashboardDataBasedOnTheFilters(@RequestBody DashboardFilterDto filters) {
        log.info("Dashboard data request received with filters: {}", filters);
        try {
            List<DashboardDto> response = dashboardService.fetchDashBoardData(filters);
            if (response == null || response.isEmpty()) {
                log.warn("Dashboard data request returned no results for filters: {}", filters);
                return ResponseEntity.noContent().build();
            }
            log.info("Dashboard data request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Dashboard data request failed for filters: {}", filters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/dashboard-summary")
    public ResponseEntity<List<DashboardDto>> getDashboardSummary() {
        log.info("Dashboard summary request received");
        try {
            List<DashboardDto> response = dashboardService.fetchDashBoardSummary();
            if (response == null || response.isEmpty()) {
                log.warn("Dashboard summary request returned no data");
                return ResponseEntity.noContent().build();
            }
            log.info("Dashboard summary request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Dashboard summary request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/historical")
    public ResponseEntity<List<HistoricalDashboardEntity>> getHistoricalDashboardData(@RequestBody HistoricalDashboardFiltersDto filters) {
        log.info("Historical dashboard POST request received with filters={}", filters);
        try {
            List<HistoricalDashboardEntity> response = dashboardService.fetchHistoricalDashboardData(filters);
            if (response == null || response.isEmpty()) {
                log.warn("Historical dashboard POST request returned no data for filters={}", filters);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Historical dashboard POST request failed for filters={}", filters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
