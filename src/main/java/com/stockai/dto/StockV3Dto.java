package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data()
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockV3Dto extends StockV2Dto {
    private double ltP;
    private double per;    //Percentage change
    private double ptsC;    //Points change
    private Long avgVol; //Avg 5 days volume
    private Double wkhi;//52weeks High
    private Double wklo;//52weeks low
    private Double volIncPer; //Volumn increment percent

    // fallback values used when AngelOne omits them in some responses
    private Double previousClose;
    private Double deliveryPercent;
    private Long deliveryQuantity;
    private Long numberOfTrades;
    private Double percentageChange;
    private Double pointsChange;
    private Double weekHigh;
    private Double weekLow;

    @JsonIgnore
    private Double distFrom52WksHi;
    @JsonIgnore
    private Double distTo52WksLo;
}
