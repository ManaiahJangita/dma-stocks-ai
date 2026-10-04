package com.stockai.controller;

import com.stockai.dto.PaymentsDto;
import com.stockai.dto.PlanDto;
import com.stockai.service.interf.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentsController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/payments-data")
    public ResponseEntity<?> fetchPaymentsBasedOnFilters(@RequestBody PaymentsDto filters) {
        if(Objects.nonNull(filters))
            return ResponseEntity.noContent().build();
        List<PaymentsDto> response = paymentService.fetchPaymentsBasedOnFilters(filters);
        if(response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentsDto>> fetchPaymentPlans() {
        List<PaymentsDto> plans = paymentService.fetchAllPayments();
        if (plans.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(plans,HttpStatus.OK);
    }
}
