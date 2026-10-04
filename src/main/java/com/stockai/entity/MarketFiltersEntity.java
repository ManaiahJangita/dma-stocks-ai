package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Data
@Table(name = "market_filters", schema = "dmastock")
public class MarketFiltersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "market_indices")
    private String market_indices;
    @Column(name = "market_performance")
    private String market_performance;
    @Column(name = "price_movement")
    private String price_movement;
    @Column(name = "volume_delivery")
    private String volume_delivery;
    @Column(name = "technical_signals")
    private String technical_signals;
    @Column(name = "breakouts")
    private String breakouts;
    @Column(name = "ai_prediction")
    private String ai_prediction;
    @Column(name = "market_sentiment")
    private String market_sentiment;
    @Column(name = "sector_performance")
    private String sector_performance;
    @Column(name = "time_period")
    private String time_period;
}
