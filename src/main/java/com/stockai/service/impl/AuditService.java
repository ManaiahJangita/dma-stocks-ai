package com.stockai.service.impl;

import com.stockai.dto.AuditLogDto;
import com.stockai.entity.AuditLogEntity;
import com.stockai.repository.AuditLogRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLogDto> getallAditLogs() {
        List<AuditLogDto> response = new ArrayList<>();
        List<AuditLogEntity> auditLogEntities = auditLogRepository.fetchAllAuditLogs();
        for(AuditLogEntity entity: auditLogEntities){
            AuditLogDto dto = new AuditLogDto();
            BeanUtils.copyProperties(entity,dto);
            response.add(dto);
        }
        return response;
    }

    public List<AuditLogDto> fetchFilterBasedLogs(AuditLogDto auditLogDto){
        List<AuditLogDto> response = new ArrayList<>();
        AuditLogEntity filterEntity = new AuditLogEntity();
        BeanUtils.copyProperties(auditLogDto,filterEntity);
        List<AuditLogEntity> auditLogEntities = auditLogRepository.fetchByFilterLogs(filterEntity);
        for(AuditLogEntity entity: auditLogEntities){
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
        return response;
    }
}