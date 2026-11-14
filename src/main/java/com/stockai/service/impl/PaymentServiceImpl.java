package com.stockai.service.impl;

import com.stockai.dto.PaymentsDto;
import com.stockai.dto.PaymentsFilterDto;
import com.stockai.entity.PaymentEntity;
import com.stockai.entity.PlanEntity;
import com.stockai.repository.PaymentRepository;
import com.stockai.repository.PlansRepository;
import com.stockai.service.interf.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PlansRepository plansRepository;

    @Override
    public List<PaymentsDto> fetchPaymentsBasedOnFilters(PaymentsFilterDto filters) {
        log.info("Fetching payment records with filters: {}", filters);
        try {
            List<PaymentEntity> response = paymentRepository.findPaymentPlans(filters);
            List<PaymentsDto> result = response.stream().map(entity -> {
                PaymentsDto dto = new PaymentsDto();
                BeanUtils.copyProperties(entity, dto);
                return dto;
            }).toList();
            log.info("Payment filter request completed with {} records", result.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch payment records with filters: {}", filters, e);
            throw e;
        }
    }

    public List<PaymentsDto> fetchAllPayments() {
        log.info("Fetching all payment plans");
        try {
            List<PaymentsDto> paymentDtos = new ArrayList<>();
            List<PaymentEntity> entities = paymentRepository.fetchAllPayments();
            for (PaymentEntity entity : entities) {
                PaymentsDto dto = new PaymentsDto();
                BeanUtils.copyProperties(entity, dto);
                paymentDtos.add(dto);
            }
            log.info("Fetched {} payment plans", paymentDtos.size());
            return paymentDtos;
        } catch (Exception e) {
            log.error("Failed to fetch all payment plans", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public synchronized int syncPaymentPlanCatalog() {
        log.info("Starting payment plan catalog sync");
        try {
            List<PlanEntity> sourcePlans = plansRepository.findAllPlans();
            log.info("Retrieved {} plans from source catalog", sourcePlans == null ? 0 : sourcePlans.size());
            List<PaymentEntity> paymentPlans = new ArrayList<>(sourcePlans.size());
            for (PlanEntity source : sourcePlans) {
                PaymentEntity target = new PaymentEntity();
                target.setPlanId(source.getId());
                target.setUserName(source.getUserName());
                target.setRecordType("SUBSCRIPTION");
                target.setPlanName(source.getName());
                target.setPlanDescription(source.getDescription());
                target.setPaymentType("Online");
                target.setAmount(BigDecimal.valueOf(source.getPrice()));
                target.setCurrency("INR");
                target.setStatus(source.getFromDate() != null && source.getFromDate().isAfter(LocalDate.now()) ? "DeActivated" : source.getStatus());
                target.setTransactionReference(source.getTransactionReference());
                target.setCreatedDate(source.getCreatedDate());
                target.setPlanUpdatedDate(source.getUpdatedDate());
                target.setFromDate(source.getFromDate());
                target.setToDate(source.getToDate());
                target.setMaximumPrice(source.getMaximumPrice());
                target.setMinimumPrice(source.getMinimumPrice());
                paymentPlans.add(target);
            }
            if (!paymentPlans.isEmpty()) {
                log.info("Syncing {} payment plans from source to target", paymentPlans.size());
                paymentRepository.upsertPaymentPlans(paymentPlans);
                log.info("Successfully synced {} payment plans from source to target", paymentPlans.size());
            } else {
                log.warn("No payment plans were available to sync");
            }
            return paymentPlans.size();
        } catch (Exception e) {
            log.error("Failed to sync payment plan catalog", e);
            throw e;
        }
    }
}
