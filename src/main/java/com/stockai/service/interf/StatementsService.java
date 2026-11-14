package com.stockai.service.interf;

import com.stockai.dto.MonthlyStatementSummaryDto;
import com.stockai.dto.StatementDto;
import com.stockai.dto.StatementFiltersDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StatementsService {

    StatementDto createStatement(StatementDto statement);

    StatementDto createSaleReferenceStatement(StatementDto statement);

    List<StatementDto> fetchStatements(StatementFiltersDto filters);

    List<StatementDto> fetchAllStatements();

    MonthlyStatementSummaryDto fetchMonthlySummary(UUID userId, LocalDate fromDate, LocalDate toDate);
}
