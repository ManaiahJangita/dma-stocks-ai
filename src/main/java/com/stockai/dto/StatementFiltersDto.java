package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatementFiltersDto {
    private String status;
    private String symbol;
    private String stockType;
    private String tradeType;
    private LocalDate fromDate;
    private LocalDate toDate;
}
