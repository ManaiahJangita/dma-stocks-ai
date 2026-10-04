package com.stockai.controller;

import com.stockai.dto.PatternDto;
import com.stockai.service.interf.PatternsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patterns")
public class PatternsController {
    @Autowired
    private PatternsService patternsService;
    @GetMapping("/patterns-data")
    public ResponseEntity<List<PatternDto>> fetchPatternsData(@ModelAttribute PatternDto filters) {
        List<PatternDto> response = patternsService.fetchPatternsData(filters);
        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
