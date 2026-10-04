package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "stock_analyzer_filters", schema = "dmastock")
public class StockAnalyzerFiltersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "stock_search")
    private String stock_search ;
    @Column(name = "stock_overview")
    private String stock_overview ;
    @Column(name = "price_analysis")
    private String price_analysis ;
    @Column(name = "technical_analysis")
    private String Technical_analysis ;
    @Column(name = "volume_analysis")
    private String volume_analysis ;
    @Column(name = "ai_prediction")
    private String ai_prediction ;
    @Column(name = "historical_performance")
    private String historical_performance ;
    @Column(name = "risk_analysis")
    private String risk_analysis ;
    @Column(name = "signals")
    private String signals ;
    @Column(name = "news_events")
    private String news_events ;
}
