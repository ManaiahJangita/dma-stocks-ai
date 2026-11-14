package com.stockai.controller;

import com.stockai.dto.CustomerDto;
import com.stockai.dto.CustomersFilterDto;
import com.stockai.service.interf.CustomersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Slf4j
public class CustomersController {
    @Autowired
    private CustomersService customersService;

    @PostMapping("/customer-data")
    public ResponseEntity<List<CustomerDto>> getCustomersBasedOnFilters(@RequestBody CustomersFilterDto filters) {
        log.info("Customer filter request received with filters: {}", filters);
        try {
            List<CustomerDto> response = customersService.getCustomersFilters(filters);
            if (response == null || response.isEmpty()) {
                log.warn("Customer filter request returned no data for filters: {}", filters);
                return ResponseEntity.noContent().build();
            }
            log.info("Customer filter request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Customer filter request failed for filters: {}", filters, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDto>> getAllCustomersInfo() {
        log.info("All customers request received");
        try {
            List<CustomerDto> response = customersService.fetchAllCustomersInfo();
            if (response == null || response.isEmpty()) {
                log.warn("All customers request returned no data");
                return ResponseEntity.noContent().build();
            }
            log.info("All customers request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("All customers request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
