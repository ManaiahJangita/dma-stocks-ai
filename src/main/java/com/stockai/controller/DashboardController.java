package com.stockai.controller;

import com.stockai.dto.DashboardDto;
import com.stockai.service.interf.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Slf4j
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard-data")
    public ResponseEntity<List<DashboardDto>> getDashboardDataBasedOnTheFilters(
            @ModelAttribute DashboardDto filters) {
        List<DashboardDto> response = dashboardService.fetchDashBoardData(filters);
        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
