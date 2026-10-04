package com.stockai.service.impl;

import com.stockai.dto.StatementDto;
import com.stockai.entity.StatementEntity;
import com.stockai.repository.StatementRepository;
import com.stockai.service.interf.StatementsService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StatementsServiceImpl implements StatementsService {
    @Autowired
    private StatementRepository statementRepository;

    @Override
    @Transactional
    public StatementDto createStatement(StatementDto request) {
        if (request == null || request.getSymbol() == null || request.getSymbol().isBlank()
                || request.getStockType() == null || request.getStockType().isBlank()
                || request.getTradeType() == null || request.getQuantity() == null || request.getUnitPrice() == null
                || request.getQuantity().signum() <= 0 || request.getUnitPrice().signum() <= 0) {
            throw new IllegalArgumentException("Symbol, stockType, BUY/SELL tradeType, positive quantity, and positive unitPrice are required");
        }

        String tradeType = request.getTradeType().trim().toUpperCase();
        if (!tradeType.equals("BUY") && !tradeType.equals("SELL")) {
            throw new IllegalArgumentException("tradeType must be BUY or SELL");
        }
        StatementEntity entity = new StatementEntity();
        entity.setUserId(request.getUserId());
        entity.setSymbol(request.getSymbol().trim().toUpperCase());
        entity.setStockType(request.getStockType().trim().toUpperCase());
        entity.setTradeType(tradeType);
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setTotalAmount(request.getTotalAmount());
        entity.setTradeDate(request.getTradeDate() == null ? LocalDate.now() : request.getTradeDate());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setStatus(request.getStatus());
        entity.setTitle(tradeType + " " + entity.getSymbol());
        entity.setDescription(request.getDescription());
        return toDto(statementRepository.saveStatement(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatementDto> fetchStatements(StatementDto filters) {
        StatementEntity entity = new StatementEntity();
        BeanUtils.copyProperties(filters,entity);
        List<StatementEntity> entities = statementRepository.findStatements(entity);
        List<StatementDto> response = new ArrayList<>();
        for(StatementEntity model: entities){
            StatementDto dto = new StatementDto();
            BeanUtils.copyProperties(model,dto);
            response.add(dto);
        }
        return response;
    }

    @Override
    public List<StatementDto> fetchAllStatements() {
       List<StatementEntity> allStatements = statementRepository.findAllStatements();
       List<StatementDto> response = new ArrayList<>();
       for(StatementEntity entity: allStatements){
           StatementDto dto = new StatementDto();
           BeanUtils.copyProperties(entity,dto);
           response.add(dto);
       }
       return response;
    }

    private StatementDto toDto(StatementEntity entity) {
        StatementDto dto = new StatementDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
