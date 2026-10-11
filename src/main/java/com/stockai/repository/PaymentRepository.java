package com.stockai.repository;

import com.stockai.dto.PaymentsFilterDto;
import com.stockai.entity.PaymentEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository {
    List<PaymentEntity> findPaymentPlans(PaymentsFilterDto filters);

    List<PaymentEntity> fetchAllPayments();

    void upsertPaymentPlans(List<PaymentEntity> plans);
}
