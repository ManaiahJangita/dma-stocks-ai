package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "market_feedback", schema = "dmastock")
public class MarketFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "market_date", nullable = false)
    private LocalDate marketDate;

    @Column(name = "actual_date", nullable = false)
    private LocalDate actualDate;

    @Column(name = "predicted_open")
    private Double predictedOpen;

    @Column(name = "predicted_high")
    private Double predictedHigh;

    @Column(name = "predicted_low")
    private Double predictedLow;

    @Column(name = "predicted_close")
    private Double predictedClose;

    @Column(name = "actual_open")
    private Double actualOpen;

    @Column(name = "actual_high")
    private Double actualHigh;

    @Column(name = "actual_low")
    private Double actualLow;

    @Column(name = "actual_close")
    private Double actualClose;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "close_diff")
    private Double closeDiff;

    @Column(name = "close_diff_percent")
    private Double closeDiffPercent;

    @Column(name = "direction_matched")
    private Boolean directionMatched;

    @Column(name = "status", length = 40)
    private String status;

    @Column(name = "retraining_feedback_score")
    private Double retrainingFeedbackScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
