package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardDto {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdDate;
    private LocalDate fromDate;
    private LocalDate toDate;
}
