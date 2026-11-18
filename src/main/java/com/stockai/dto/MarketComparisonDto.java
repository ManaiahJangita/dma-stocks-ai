package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketComparisonDto {
    private UUID id;
    private String symbol;
    private UUID marketRunId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate marketDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate actualDate;

    private Double predictedOpen;
    private Double predictedHigh;
    private Double predictedLow;
    private Double predictedClose;
    private String predictedDirection;

    private Double actualOpen;
    private Double actualHigh;
    private Double actualLow;
    private Double actualClose;
    private String actualDirection;

    private Double closeDiff;
    private Double highDiff;
    private Double lowDiff;
    private Double closeDiffPercent;
    private Double highDiffPercent;
    private Double lowDiffPercent;

    private Boolean directionMatched;
    private Boolean actualVsPredictedDirectionMatch;
    private String status;
    private String reason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate createdAt;

    private Double predictedProbabilityUp;
    private Double predictedProbabilityDown;
    private Double confidencePercent;
    private Double predictedHighChangePct;
    private Double predictedLowChangePct;
    private Double predictedCloseChangePct;
    private Double modelValidationAccuracyPercent;
    private Double retrainingFeedbackScore;
    private Boolean targetReached;
    private Boolean sellRecommended;
    private String targetStatus;
}
