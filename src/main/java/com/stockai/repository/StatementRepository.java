package com.stockai.repository;

import com.stockai.dto.MonthlyStatementSummaryDto;
import com.stockai.dto.StatementFiltersDto;
import com.stockai.entity.StatementEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StatementRepository {
    List<StatementEntity> findAllStatements();

    StatementEntity saveStatement(StatementEntity statement);

    List<StatementEntity> findStatements(StatementFiltersDto filters);

    MonthlyStatementSummaryDto getMonthlyStatementSummary(UUID userId, LocalDate fromDate, LocalDate toDate);
}
