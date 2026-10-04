package com.stockai.repository;

import com.stockai.entity.PaymentEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository {
    List<PaymentEntity> findPaymentPlans(PaymentEntity filters);

    List<PaymentEntity> fetchAllPayments();

    void upsertPaymentPlans(List<PaymentEntity> plans);
}
