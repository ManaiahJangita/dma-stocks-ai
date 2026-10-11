package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "patterns", schema = "dmastock")
public class PatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

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

    @Column(name = "pattern_name", length = 120)
    private String patternName;

    @Column(name = "pattern_type", length = 80)
    private String patternType;

    @Column(name = "signal_type", length = 80)
    private String signalType;

    @Column(name = "volume_ratio")
    private Double volumeRatio;

    @Column(name = "sma_20")
    private Double sma20;

    @Column(name = "sma_50")
    private Double sma50;

    @Column(name = "rsi_14")
    private Double rsi14;

    @Column(name = "trend_label", length = 40)
    private String trendLabel;

    @Column(name = "breakout_flag")
    private Boolean breakoutFlag;

    @Column(name = "score")
    private Double score;

    @Column(name = "filter_type", length = 80)
    private String filterType;
}
