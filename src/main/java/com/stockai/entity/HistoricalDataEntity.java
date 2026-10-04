package com.stockai.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "historical_data", schema = "dmastock")
public class HistoricalDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historical_data_seq")
    @SequenceGenerator(name = "historical_data_seq", sequenceName = "historical_data_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private double open;

    @Column(nullable = false)
    private double high;

    @Column(nullable = false)
    private double low;

    @Column(nullable = false)
    private double close;

    @Column(nullable = false)
    private long volume;

    @Column(nullable = false)
    private double per;

    @Column(nullable = false)
    private double ptsC;   //Percentage change

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;
}