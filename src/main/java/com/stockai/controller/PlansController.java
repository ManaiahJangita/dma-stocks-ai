package com.stockai.controller;

import com.stockai.dto.PlanDto;
import com.stockai.service.interf.PlansService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/plans")
public class PlansController {
    @Autowired
    private PlansService plansService;

    @PostMapping("/save-plans")
    public ResponseEntity<String> savePlan(@RequestBody PlanDto planDto) {
        if(Objects.isNull(planDto)) {
            return ResponseEntity.badRequest().body("Plan data is required");
        }
        String response = plansService.addPlan(planDto);
        if (!StringUtils.hasText(response)) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
