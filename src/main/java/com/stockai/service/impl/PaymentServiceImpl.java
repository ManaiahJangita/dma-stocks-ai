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
        List<PaymentEntity> response = paymentRepository.findPaymentPlans(filters);
        return response.stream().map(entity -> {;
            PaymentsDto dto = new PaymentsDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).toList();
    }

    public List<PaymentsDto> fetchAllPayments() {
        List<PaymentsDto> paymentDtos = new ArrayList<>();
        List<PaymentEntity> entities = paymentRepository.fetchAllPayments();
        for (PaymentEntity entity : entities) {
            PaymentsDto dto = new PaymentsDto();
            BeanUtils.copyProperties(entity, dto);
            paymentDtos.add(dto);
        }
        return paymentDtos;
    }

    @Override
    @Transactional
    public synchronized int syncPaymentPlanCatalog() {
        List<PlanEntity> sourcePlans = plansRepository.findAllPlans();
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
        }
        return paymentPlans.size();
    }
}
