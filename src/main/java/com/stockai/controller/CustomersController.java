package com.stockai.controller;

import com.stockai.dto.CustomerDto;
import com.stockai.service.interf.CustomersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomersController {
    @Autowired
    private CustomersService customersService;

    @GetMapping("/customer-data")
    public ResponseEntity<List<CustomerDto>> getCustomersBasedOnFilters(@RequestBody CustomerDto filters) {
        List<CustomerDto> response = customersService.getCustomersFilters(filters);
        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDto>> getAllCustomersInfo(){
        List<CustomerDto> response = customersService.fetchAllCustomersInfo();
        if(response.isEmpty())
            return ResponseEntity.noContent().build();
        return new ResponseEntity<>(response,HttpStatus.OK);
    }
}
