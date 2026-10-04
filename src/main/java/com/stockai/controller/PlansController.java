package com.stockai.controller;

import com.stockai.dto.PlanDto;
import com.stockai.service.interf.PlansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plans")
public class PlansController {
    @Autowired
    private PlansService plansService;

    @PostMapping("/save-plans")
    public ResponseEntity<String> savePlan(@RequestBody PlanDto planDto) {
        if (planDto.getMinimumPrice() != null && planDto.getMaximumPrice() != null
                && planDto.getMinimumPrice() > planDto.getMaximumPrice()) {
            return ResponseEntity.badRequest().body("minimumPrice must not exceed maximumPrice");
        }
        String response = plansService.addPlan(planDto);
        if (!StringUtils.hasText(response)) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
