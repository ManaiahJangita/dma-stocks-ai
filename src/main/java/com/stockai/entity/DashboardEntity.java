package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "dashboard", schema = "dmastock")
public class DashboardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;
}
