package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "markets", schema = "dmastock")
public class MarketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column
    private String status;

    @Column(name = "historical_data_id", unique = true)
    private Long historicalDataId;

    @Column
    private String symbol;

    private Double open;
    private Double high;
    private Double low;
    private Double close;

    @Column(name = "previous_close")
    private Double previousClose;

    private Long volume;
    private Double per;
    private Double ptsC;

    @Column(name = "market_date")
    private LocalDate marketDate;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Transient
    private LocalDate fromDate;

    @Transient
    private LocalDate toDate;

    @Transient
    private String recordType;

    @Transient
    private String predictionDirectionFilter;

    @Transient
    private Double minimumConfidence;

    @Transient
    private Double maximumConfidence;
}
