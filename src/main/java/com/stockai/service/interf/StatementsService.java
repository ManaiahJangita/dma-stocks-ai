package com.stockai.service.interf;

import com.stockai.dto.StatementDto;

import java.util.List;

public interface StatementsService {

    StatementDto createStatement(StatementDto statement);

    List<StatementDto> fetchStatements(StatementDto filters);

    List<StatementDto> fetchAllStatements();
}
