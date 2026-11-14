package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstrumentsDto {
    private Long id;
    private String token;
    private String symbol;
    private String name;
    private Double strike;
    private String instrumentType;
    private String exchSeg;
    private Integer lotSize;
    private Double tickSize;
    private LocalDateTime createdDate;
}
