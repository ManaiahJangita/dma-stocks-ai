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
public class PlanDto {
    private UUID id;
    private String name;
    private String userName;
    private String transactionReference;
    private String description;
    private Double price;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Double minimumPrice;
    private Double maximumPrice;
}
