package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "plans", schema = "dmastock")
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "transaction_reference", length = 120)
    private String transactionReference;

    @Column
    private String description;

    @Column(nullable = false)
    private double price;

    @Column
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;

    @Transient
    private Double minimumPrice;

    @Transient
    private Double maximumPrice;
}
