/**
 *
 */
package com.stockai.entity;

/**
 * This is generic candle regardless of the intraday or positional.
 *
 * @author sudhakar.badugu Dec 3, 2019
 */
public interface Candle {

    /**
     * Returns the candle open price.
     *
     * @return
     */
    public double getCandleOpen();

    /**
     * Returns the candle low price.
     *
     * @return
     */
    public double getCandleLow();

    /**
     * Returns the candle high price.
     *
     * @return
     */
    public double getCandleHigh();

    /**
     * Returns the candle close price.
     *
     * @return
     */
    public double getCandleClose();

}
