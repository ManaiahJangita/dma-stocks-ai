package com.stockai.service.interf;

import com.stockai.dto.StatementDto;
import com.stockai.dto.StatementFiltersDto;

import java.util.List;

public interface StatementsService {

    StatementDto createStatement(StatementDto statement);

    List<StatementDto> fetchStatements(StatementFiltersDto filters);

    List<StatementDto> fetchAllStatements();
}
