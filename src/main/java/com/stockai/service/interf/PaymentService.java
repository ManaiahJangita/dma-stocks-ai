package com.stockai.service.interf;

import com.stockai.dto.PaymentsDto;
import com.stockai.dto.PlanDto;

import java.util.List;

public interface PaymentService {
    List<PaymentsDto> fetchPaymentsBasedOnFilters(PaymentsDto filters);

    List<PaymentsDto> fetchAllPayments();

    int syncPaymentPlanCatalog();
}
