package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDto {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private String roles;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
