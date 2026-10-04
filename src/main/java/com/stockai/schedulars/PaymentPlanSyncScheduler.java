package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentPlanSyncScheduler extends BasicJob {
    private final PaymentService paymentService;

    public PaymentPlanSyncScheduler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @Override
    @Scheduled(cron = "0 0 17 ? * MON-FRI")
    public void executeJob() {
        log.info("Execute Job Method Execution started to load all payment details.");
        if(canProcess()) {
            try {
                int synced = paymentService.syncPaymentPlanCatalog();
                log.info("Synchronized {} plans into the payment plan catalog", synced);
            } catch (Exception exception) {
                log.error("Could not synchronize plans into the payment plan catalog", exception);
            }
        }
    }
}