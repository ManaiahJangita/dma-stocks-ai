package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Table(name = "prediction", schema = "dmastock")
public class PredictionEntity {
    @Id
    @GeneratedValue(generator = "prediction_generator", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "prediction_generator", sequenceName = "dmastock.prediction_sequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column(name = "run_id")
    private UUID runId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Double open;

    @Column(nullable = false)
    private Double high;

    @Column(nullable = false)
    private Double low;

    @Column(nullable = false)
    private Double day_end_close;

    @Column(nullable = false)
    private Double previous_close;

    @Column(nullable = false)
    private Double trd_vol;

    @Column(nullable = false)
    private double per;

    @Column(nullable = false)
    private double ptsC;

    @Column(nullable = false)
    private LocalDate created_date;

    @Column(nullable = false)
    private String prediction_date;

    @Column(nullable = false)
    private double prediction_close;

    @Column(nullable = false)
    private String prediction_direction;

    @Column(nullable = false)
    private double prediction_close_percentage;

    @Column(nullable = false)
    private double predicted_high;

    @Column(nullable = false)
    private double predicted_high_percentage;

    @Column(nullable = false)
    private double predicted_low;

    @Column(nullable = false)
    private double predicted_low_percentage;

    @Column(nullable = false)
    private double confidence_percent;

    @Column(nullable = false)
    private double up_probability_percent;

    @Column(nullable = false)
    private double down_probability_percent;
    
    @Transient
    private LocalDate fromDate;
    
    @Transient
    private LocalDate toDate;
    
    @Transient
    private Double minimumConfidence;
    
    @Transient
    private Double maximumConfidence;

    @Transient
    private String predictionDirectionFilter;
}
