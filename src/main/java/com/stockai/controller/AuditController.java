package com.stockai.controller;

import com.stockai.dto.AuditLogDto;
import com.stockai.service.impl.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit-summary")
    public ResponseEntity<List<AuditLogDto>> getAuditSummary() {
        List<AuditLogDto> response = auditService.getallAditLogs();
        if(response.isEmpty())
            return ResponseEntity.noContent().build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/audit-summary-filter")
    public ResponseEntity<List<AuditLogDto>> getAuditSummaryFilter(@RequestBody AuditLogDto auditLogDto) {
        List<AuditLogDto> response = auditService.fetchFilterBasedLogs(auditLogDto);
        if(response.isEmpty())
            return ResponseEntity.noContent().build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}