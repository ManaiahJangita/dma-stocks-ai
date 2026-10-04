package com.stockai.schedulars;

import com.stockai.schedulars.job.BasicJob;
import com.stockai.service.interf.CustomersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomersSyncSchedular extends BasicJob {

    @Autowired
    private CustomersService customersService;
    @Override
    @Scheduled(cron = "0 30 16 ? * MON-SAT")
    public void executeJob() {
        log.info("Customers sync job execution started.");
        int count = customersService.syncAllCustomersInfo();
        log.info("Customers sync job execution completed: {}.",count);
    }
}
