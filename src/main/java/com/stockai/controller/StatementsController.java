package com.stockai.controller;

import com.stockai.dto.StatementDto;
import com.stockai.dto.StatementFiltersDto;
import com.stockai.service.interf.StatementsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statements")
@Slf4j
public class StatementsController {
    @Autowired
    private StatementsService statementsService;

    @PostMapping("/save")
    public ResponseEntity<?> createStatement(@RequestBody StatementDto statement) {
        try {
            StatementDto created = statementsService.createStatement(statement);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/filter-data")
    public ResponseEntity<List<StatementDto>> fetchFilteredStatements(@RequestBody StatementFiltersDto filters) {
        log.info("Fetching statements with filters: {}", filters);
        List<StatementDto> response = statementsService.fetchStatements(filters);
        if (response.isEmpty())
            return ResponseEntity.noContent().build();
        log.info("Fetched {} statements with filters: {}", response.size(), filters);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping("/all")
    public ResponseEntity<?> fetchAllStatements() {
        log.info("Fetching all statements");
        List<StatementDto> response = statementsService.fetchAllStatements();
        if (response.isEmpty())
            return ResponseEntity.notFound().build();
        log.info("Fetched {} statements in total", response.size());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
