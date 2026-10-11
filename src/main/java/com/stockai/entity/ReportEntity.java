package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "reports", schema = "dmastock")
public class ReportEntity {

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

    @Column(name = "filter_value", length = 120)
    private String filterValue;

    @Column(length = 60)
    private String symbol;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "change_percent")
    private Double changePercent;

    @Column(name = "close_price")
    private Double closePrice;

    @Column
    private Long volume;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "data_date")
    private LocalDate dataDate;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;
}
