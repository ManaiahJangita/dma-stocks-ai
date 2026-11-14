package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonthlyStatementSummaryDto {
    private UUID userId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal totalBuyAmount = BigDecimal.ZERO;
    private BigDecimal totalSellAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private Long buyRecordsCount = 0L;
    private Long sellRecordsCount = 0L;
}

