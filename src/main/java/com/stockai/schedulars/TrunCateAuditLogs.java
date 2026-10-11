package com.stockai.schedulars;

import com.stockai.repository.AuditLogRepository;
import com.stockai.schedulars.job.BasicJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrunCateAuditLogs extends BasicJob {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Scheduled(cron = "0 0 0 1 */3 *")
    public void executeJob() {
        log.info("Executing Truncate audit log table.");
        auditLogRepository.truncateAudiLogs();
        log.info("");
    }
}
