package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "reports_filters", schema = "dmastock")
public class ReportFiltersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "reports" ,nullable = false)
    private String reports;
}
