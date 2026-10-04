package com.stockai.controller;

import com.stockai.dto.ReportsDto;
import com.stockai.service.interf.ReportsService;
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
@RequestMapping("/api/v1/reports")
@Slf4j
public class ReportsController {
    @Autowired
    private ReportsService reportsService;

    @GetMapping("/reports-data")
    public ResponseEntity<List<ReportsDto>> fetchReportsBasedOnFilters(@ModelAttribute ReportsDto filters) {
        List<ReportsDto> response = reportsService.fetchReportsBasedOnFilters(filters);
        if(response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
