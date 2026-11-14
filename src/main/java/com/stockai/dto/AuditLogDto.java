package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogDto {
    private UUID id;
    private UUID userId;
    private String email;
    private String eventType;
    private LocalDateTime eventAt;
    private LocalDateTime createdDate;
    private LocalDate updatedDate;

}