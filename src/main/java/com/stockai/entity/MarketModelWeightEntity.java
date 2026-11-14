package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "market_model_weight", schema = "dmastock")
public class MarketModelWeightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "model_version_id", nullable = false, columnDefinition = "uuid")
    private UUID modelVersionId;

    @Column(name = "signal_name", nullable = false, length = 50)
    private String signalName;

    @Column(name = "weight_value", nullable = false)
    private Double weightValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

