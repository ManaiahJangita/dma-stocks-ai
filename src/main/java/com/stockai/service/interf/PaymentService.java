package com.stockai.service.interf;

import com.stockai.dto.PaymentsDto;
import com.stockai.dto.PaymentsFilterDto;
import com.stockai.dto.PlanDto;

import java.util.List;

public interface PaymentService {
    List<PaymentsDto> fetchPaymentsBasedOnFilters(PaymentsFilterDto filters);

    List<PaymentsDto> fetchAllPayments();

    int syncPaymentPlanCatalog();
}
