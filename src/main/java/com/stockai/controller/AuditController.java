package com.stockai.controller;

import com.stockai.dto.AuditLogDto;
import com.stockai.service.impl.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Slf4j
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit-summary")
    public ResponseEntity<List<AuditLogDto>> getAuditSummary() {
        log.info("Audit summary request received");
        try {
            List<AuditLogDto> response = auditService.getallAditLogs();
            if (response == null || response.isEmpty()) {
                log.warn("Audit summary request returned no data");
                return ResponseEntity.noContent().build();
            }
            log.info("Audit summary request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Audit summary request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/audit-summary-filter")
    public ResponseEntity<List<AuditLogDto>> getAuditSummaryFilter(@RequestBody AuditLogDto auditLogDto) {
        log.info("Audit filter request received with payload: {}", auditLogDto);
        try {
            List<AuditLogDto> response = auditService.fetchFilterBasedLogs(auditLogDto);
            if (response == null || response.isEmpty()) {
                log.warn("Audit filter request returned no data for payload: {}", auditLogDto);
                return ResponseEntity.noContent().build();
            }
            log.info("Audit filter request succeeded with {} records", response.size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Audit filter request failed for payload: {}", auditLogDto, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}