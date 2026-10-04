package com.stockai.data.feed;

import com.angelbroking.smartapi.models.Instrument;
import com.stockai.dto.StockV3Dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Sudhakar.Badugu
 * 05-03-2023
 */
public interface AngelOneRestService {

    void login();

    void logout();

    StockV3Dto getOHLCQuote(String symbol);

    List<StockV3Dto> getOHLCQuote(List<String> symbols);

    List<StockV3Dto> getHistoricalData(String symbol, AngelOneTimeFrameInterval timeFrame);

    List<StockV3Dto> getHistoricalData(String symbol, AngelOneTimeFrameInterval timeFrame, LocalDateTime startDate,
                                       LocalDateTime endDate);
}
