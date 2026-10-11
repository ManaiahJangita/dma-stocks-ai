package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "instruments", schema = "dmastock")
@Data
public class InstrumentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "instrument_seq_gen")
    @SequenceGenerator(name = "instrument_seq_gen", sequenceName = "instrument_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double strike = 0.0;

    @Column(name = "instrument_type")
    private String instrumentType;

    @Column(name = "exch_seg", nullable = false)
    private String exchSeg;

    @Column(name = "lot_size", nullable = false)
    private Integer lotSize;

    @Column(name = "tick_size", nullable = false)
    private Double tickSize;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}