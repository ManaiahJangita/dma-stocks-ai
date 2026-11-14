package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private String type;
    private Boolean read;
    private String priority;
    private String referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

