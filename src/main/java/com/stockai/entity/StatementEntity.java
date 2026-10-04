package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "statements", schema = "dmastock")
public class StatementEntity {

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

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 80)
    private String symbol;

    @Column(name = "stock_type", length = 40)
    private String stockType;

    @Column(name = "trade_type", length = 10)
    private String tradeType;

    @Column(precision = 20, scale = 6)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 20, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 20, scale = 6)
    private BigDecimal totalAmount;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;

    @Transient
    private Integer page;

    @Transient
    private Integer size;
}
