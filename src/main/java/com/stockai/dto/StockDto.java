package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.stockai.entity.Candle;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockDto implements Candle {
    private Long id;
    private String symbol;
    private String series;
    private String nameOfCompany;

    // intraday properties
    private String open;
    private String high;
    private String low;
    private String ltP;// closing price
    private String previousClose;
    private String dayEndClose;

    // day properties
    private Double dayOpen;
    private Double dayHigh;
    private Double dayLow;
    private Double lastPrice;
    private Double yearHigh;
    private Double yearLow;
    private Double nearWKH;
    private Double nearWKL;
    private Double perChange365d;
    private Double perChange30d;


    private Double change; // today change
    private Double pChange; // today % change
    private Double wper; // week % change
    private String totalTradedVolume;

    private String ptsC; // today change
    private String per; // today % change
    private String trdVol;
    private String wkhi;
    private String wklo;
    private String mPC;
    private String yPC;
    private Double wkPer;    //Performance form 52weeks low
    private Double distFrom52Wkhi;    //Distance from 52weeks high.

    private String priceAction;
    private Double stopLoss;
    private Double profit;
    private String intraDayResult; // result could be stoploss hit/target achieved.
    private LocalDateTime created_date;
    private double trueRange;    //Average range (high-low) for certain period.
    private int stockScore;    //Propertiy score
    private int previousScore;    //Propertiy score

    @Override
    public double getCandleOpen() {
        return 0;
    }

    @Override
    public double getCandleLow() {
        return 0;
    }

    @Override
    public double getCandleHigh() {
        return 0;
    }

    @Override
    public double getCandleClose() {
        return 0;
    }
//	private PivotPointDto dailyPivotPoint;
//	private PivotPointDto weeklyPivotPoint;
//	private PivotPointDto monthlyPivotPoint;
//	private PivotPointDto yearlyPivotPoint;
//	private StockTradeSignalDataDto tradeSignalData;
//
//	@JsonIgnore
//	private Set<PatternDataDto> patterns;
//	private Set<StockBulkDealsDto> bulkDeals;
//	private SupportAndResistanceSummaryDto supportAndResistanceSummary;
//	private SupportAndResistanceSummaryDto swingSupportAndResistance;
//
}
