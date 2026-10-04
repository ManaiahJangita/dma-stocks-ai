package com.stockai.repository;

import com.stockai.entity.AuditLogEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository {
    List<AuditLogEntity> fetchByFilterLogs(AuditLogEntity auditLogEntity);

    List<AuditLogEntity> fetchAllAuditLogs();

    AuditLogEntity saveAuditLogEntity(AuditLogEntity auditLogEntity);

    void truncateAudiLogs();

}