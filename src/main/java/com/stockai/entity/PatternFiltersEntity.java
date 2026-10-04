package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "pattern_filters", schema = "dmastock")
public class PatternFiltersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "reports", nullable = false, unique = true)
    private String patternName;
}
