package com.stockai.service.impl;

import com.stockai.dto.AuditLogDto;
import com.stockai.entity.AuditLogEntity;
import com.stockai.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLogDto> getallAditLogs() {
        log.info("Fetching all audit logs");
        List<AuditLogDto> response = new ArrayList<>();
        try {
            List<AuditLogEntity> auditLogEntities = auditLogRepository.fetchAllAuditLogs();
            log.info("Fetched {} audit log records", auditLogEntities == null ? 0 : auditLogEntities.size());
            for (AuditLogEntity entity : auditLogEntities) {
                AuditLogDto dto = new AuditLogDto();
                BeanUtils.copyProperties(entity, dto);
                response.add(dto);
            }
            log.info("Completed mapping {} audit log records to DTOs", response.size());
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch all audit logs", e);
            throw e;
        }
    }

    public List<AuditLogDto> fetchFilterBasedLogs(AuditLogDto auditLogDto) {
        log.info("Fetching audit logs with filters: {}", auditLogDto);
        List<AuditLogDto> response = new ArrayList<>();
        try {
            AuditLogEntity filterEntity = new AuditLogEntity();
            BeanUtils.copyProperties(auditLogDto, filterEntity);
            List<AuditLogEntity> auditLogEntities = auditLogRepository.fetchByFilterLogs(filterEntity);
            log.info("Repository returned {} filtered audit log records", auditLogEntities == null ? 0 : auditLogEntities.size());
            for (AuditLogEntity entity : auditLogEntities) {
                AuditLogDto dto = new AuditLogDto();
                dto.setId(entity.getId());
                dto.setEmail(entity.getEmail());
                dto.setCreatedDate(entity.getCreatedDate());
                dto.setEventAt(entity.getEventAt());
                dto.setUserId(entity.getUserId());
                dto.setEventType(entity.getEvent_type());
                dto.setUpdatedDate(entity.getUpdatedDate());
                response.add(dto);
            }
            log.info("Filtered audit logs processed successfully. Total DTOs: {}", response.size());
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch filtered audit logs for filter {}", auditLogDto, e);
            throw e;
        }
    }
}