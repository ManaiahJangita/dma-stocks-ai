package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "market_model_version", schema = "dmastock")
public class MarketModelVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "version_label", nullable = false, length = 100)
    private String versionLabel;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "base_version_id", columnDefinition = "uuid")
    private UUID baseVersionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "total_comparisons")
    private Integer totalComparisons;

    @Column(name = "average_accuracy_percent")
    private Double averageAccuracyPercent;

    @Column(name = "notes", length = 500)
    private String notes;
}

