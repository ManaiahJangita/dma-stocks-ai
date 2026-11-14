package com.stockai.service.interf;

import com.stockai.entity.MarketComparisonEntity;
import com.stockai.entity.MarketFeedbackEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MarketFeedbackService {

    void saveFeedback(MarketComparisonEntity comparison);

    void saveBulkFeedback(List<MarketComparisonEntity> comparisons);

    List<MarketFeedbackEntity> getUnprocessedFeedback();

    List<MarketFeedbackEntity> getFailedFeedback();

    void markProcessed(List<UUID> ids);

    void markProcessedByMarketDate(LocalDate marketDate);

    void deleteProcessedEntriesOlderThan(LocalDateTime cutoff);
}

