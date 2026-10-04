package com.stockai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockai.utils.MathUtil;
import com.stockai.utils.StockUtil;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_stock" , schema = "dmastock")
@JsonIgnoreProperties
@Data
public class DailyStockModel implements Candle {
    @Id
    @GeneratedValue(generator = "daily_stock_generator")
    @SequenceGenerator(name = "daily_stock_generator", sequenceName = "daily_stock_sequence", initialValue = 1)
    private Long id;

    @Column
    private String symbol;

    @Column
    private String open;
    private String high;
    private String low;
    private String ltP;
    private String ptsC; // today change
    private String per; // today % change
    private String trdVol;    //Trade volume today
    private String deliveryQuntity;
    private String deliveryPercent;
    private String noOfTrades;
    private String wkhi;
    private String wklo;
    private String previousClose;
    private String dayEndClose;

    //	@Transient
//	private CandleType candleType = CandleType.DAY;
    private LocalDate created_date;
    private LocalDateTime updated_time = LocalDateTime.now();

    @Transient
    public boolean isDojiCandle() {
        double dayEndClose = getCandleClose();
        double open = getCandleOpen();
        double range = Math.abs(dayEndClose - open);
        return dayEndClose == open || (range < 1);
    }

    @Transient
    public boolean isBullishCandle() {
        double dayEndClose = MathUtil.parseDouble(getDayEndClose());
        double open = getCandleOpen();

        return dayEndClose >= open;
    }

    @Transient
    public boolean isBearishCandle() {
        double firstCandleClose = MathUtil.parseDouble(getDayEndClose());
        double firstCandleOpen = getCandleOpen();
        return firstCandleClose < firstCandleOpen;
    }

    @Transient
    public Double getTypicalPrice() {
        Double typicalPrice = 0.0;
        try {
            double high = MathUtil.parseDouble(getHigh());
            double low = MathUtil.parseDouble(getLow());
            double close = MathUtil.parseDouble(getDayEndClose());
            typicalPrice = MathUtil.roundToTwoDigits((high + low + close) / 3);
        } catch (Exception e) {

        }

        return typicalPrice;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DailyStockModel [");
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (created_date != null) {
            builder.append("Date=");
            builder.append(created_date);
            builder.append(", ");
        }
        if (symbol != null) {
            builder.append("symbol=");
            builder.append(symbol);
            builder.append(", ");
        }
        if (open != null) {
            builder.append("open=");
            builder.append(open);
            builder.append(", ");
        }
        if (high != null) {
            builder.append("high=");
            builder.append(high);
            builder.append(", ");
        }
        if (low != null) {
            builder.append("low=");
            builder.append(low);
            builder.append(", ");
        }
        if (dayEndClose != null) {
            builder.append("dayEndClose=");
            builder.append(dayEndClose);
            builder.append(", ");
        }
        if (ltP != null) {
            builder.append("ltP=");
            builder.append(ltP);
        }
        builder.append("]");
        return builder.toString();
    }

    @Transient
    @Override
    public double getCandleOpen() {
        return MathUtil.parseDouble(open);
    }

    @Transient
    @Override
    public double getCandleLow() {
        return MathUtil.parseDouble(low);
    }

    @Transient
    @Override
    public double getCandleHigh() {
        return MathUtil.parseDouble(high);
    }

    @Transient
    @Override
    public double getCandleClose() {
        return StockUtil.getDayEndPriceOrLtp(this);
    }

    @Transient
    public double getCandleRange() {
        return getCandleHigh() - getCandleLow();
    }

}
