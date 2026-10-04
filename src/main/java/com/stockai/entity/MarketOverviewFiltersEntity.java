package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Data
@Table(name = "market_overview_filters", schema = "dmastock")
public class MarketOverviewFiltersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "market_summary")
    private String market_summary;
    @Column(name = "market_movers")
    private String market_movers ;
    @Column(name = "market_status")
    private String market_status;
    @Column(name = "market_signals")
    private String market_signals;
    @Column(name = "ai_predictions_summary")
    private String ai_predictions_summary ;
    @Column(name = "sector_performance")
    private String sector_performance ;
    @Column(name = "mywatchlist_summary")
    private String mywatchlist_summary ;
}
