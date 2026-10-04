package com.stockai.controller;

import com.stockai.dto.StatementDto;
import com.stockai.service.interf.StatementsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statements")
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

    @GetMapping("/filter-data")
    public ResponseEntity<List<StatementDto>> fetchFilteredStatements(@RequestBody StatementDto filters) {
        List<StatementDto> response = statementsService.fetchStatements(filters);
        if (response.isEmpty())
            return ResponseEntity.noContent().build();
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping("/all")
    public ResponseEntity<?> fetchAllStatements() {
        List<StatementDto> response = statementsService.fetchAllStatements();
        if (response.isEmpty())
            return ResponseEntity.notFound().build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
