package com.stockai.service.impl;

import com.stockai.entity.MarketComparisonEntity;
import com.stockai.entity.MarketFeedbackEntity;
import com.stockai.repository.MarketFeedbackRepository;
import com.stockai.service.interf.MarketFeedbackService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketFeedbackServiceImpl implements MarketFeedbackService {

    private final MarketFeedbackRepository marketFeedbackRepository;

    @Override
    public void saveFeedback(MarketComparisonEntity comparison) {
        if (comparison == null) {
            return;
        }

        MarketFeedbackEntity feedback = new MarketFeedbackEntity();
        feedback.setSymbol(comparison.getSymbol());
        feedback.setMarketDate(comparison.getMarketDate());
        feedback.setActualDate(comparison.getActualDate());
        feedback.setPredictedOpen(comparison.getPredictedOpen());
        feedback.setPredictedHigh(comparison.getPredictedHigh());
        feedback.setPredictedLow(comparison.getPredictedLow());
        feedback.setPredictedClose(comparison.getPredictedClose());
        feedback.setActualOpen(comparison.getActualOpen());
        feedback.setActualHigh(comparison.getActualHigh());
        feedback.setActualLow(comparison.getActualLow());
        feedback.setActualClose(comparison.getActualClose());
        feedback.setCloseDiff(comparison.getCloseDiff());
        feedback.setCloseDiffPercent(comparison.getCloseDiffPercent());
        feedback.setDirectionMatched(comparison.getDirectionMatched());
        feedback.setStatus(comparison.getStatus());
        feedback.setRetrainingFeedbackScore(comparison.getRetrainingFeedbackScore());
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setProcessed(false);

        marketFeedbackRepository.save(feedback);
    }

    @Override
    public void saveBulkFeedback(List<MarketComparisonEntity> comparisons) {
        if (comparisons == null || comparisons.isEmpty()) {
            return;
        }

        List<MarketFeedbackEntity> feedbackList = new ArrayList<>();
        for (MarketComparisonEntity comparison : comparisons) {
            if (comparison == null) {
                continue;
            }

            MarketFeedbackEntity feedback = new MarketFeedbackEntity();
            feedback.setSymbol(comparison.getSymbol());
            feedback.setMarketDate(comparison.getMarketDate());
            feedback.setActualDate(comparison.getActualDate());
            feedback.setPredictedOpen(comparison.getPredictedOpen());
            feedback.setPredictedHigh(comparison.getPredictedHigh());
            feedback.setPredictedLow(comparison.getPredictedLow());
            feedback.setPredictedClose(comparison.getPredictedClose());
            feedback.setActualOpen(comparison.getActualOpen());
            feedback.setActualHigh(comparison.getActualHigh());
            feedback.setActualLow(comparison.getActualLow());
            feedback.setActualClose(comparison.getActualClose());
            feedback.setCloseDiff(comparison.getCloseDiff());
            feedback.setCloseDiffPercent(comparison.getCloseDiffPercent());
            feedback.setDirectionMatched(comparison.getDirectionMatched());
            feedback.setStatus(comparison.getStatus());
            feedback.setRetrainingFeedbackScore(comparison.getRetrainingFeedbackScore());
            feedback.setCreatedAt(LocalDateTime.now());
            feedback.setProcessed(false);
            feedbackList.add(feedback);
        }

        if (!feedbackList.isEmpty()) {
            marketFeedbackRepository.saveAll(feedbackList);
        }
    }

    @Override
    public List<MarketFeedbackEntity> getUnprocessedFeedback() {
        return marketFeedbackRepository.findByProcessedFalseOrderByCreatedAtAsc();
    }

    @Override
    public List<MarketFeedbackEntity> getFailedFeedback() {
        return marketFeedbackRepository.findByProcessedFalseAndDirectionMatchedFalseOrderByCreatedAtAsc();
    }

    @Override
    @Transactional
    public void markProcessed(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        marketFeedbackRepository.markProcessed(ids, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markProcessedByMarketDate(LocalDate marketDate) {
        if (marketDate == null) {
            return;
        }
        marketFeedbackRepository.markProcessedByMarketDate(marketDate, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteProcessedEntriesOlderThan(LocalDateTime cutoff) {
        if (cutoff == null) {
            return;
        }
        marketFeedbackRepository.deleteByProcessedTrueAndProcessedAtBefore(cutoff);
    }
}

