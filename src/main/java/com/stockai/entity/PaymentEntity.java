package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "payments", schema = "dmastock")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "record_type", length = 32)
    private String recordType = "PAYMENT";

    @Column(name = "plan_name", length = 160)
    private String planName;

    @Column(name = "plan_description", length = 1000)
    private String planDescription;

    @Column(name = "plan_updated_date")
    private LocalDateTime planUpdatedDate;

    @Column(name = "payment_type", nullable = false, length = 40)
    private String paymentType;

    @Column(name = "userName", nullable = false, length = 40)
    private String userName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "transaction_reference", length = 120)
    private String transactionReference;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Transient
    private LocalDate fromDate;
    
    @Transient
    private LocalDate toDate;
    
    @Transient
    private BigDecimal minimumAmount;
    
    @Transient
    private BigDecimal maximumAmount;

    @Transient
    private Double minimumPrice;

    @Transient
    private Double maximumPrice;
}