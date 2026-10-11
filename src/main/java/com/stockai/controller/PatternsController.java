package com.stockai.controller;

import com.stockai.dto.PatternDto;
import com.stockai.dto.PatternFilterDto;
import com.stockai.service.interf.PatternsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patterns")
public class PatternsController {
    @Autowired
    private PatternsService patternsService;

    @PostMapping("/patterns-data")
    public ResponseEntity<List<PatternDto>> fetchPatternsData(@RequestBody PatternFilterDto filters) {
        List<PatternDto> response = patternsService.fetchPatternsData(filters);
        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
