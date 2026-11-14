package com.stockai.repository;

import com.stockai.entity.MarketComparisonEntity;
import com.stockai.entity.MarketComparisonHistoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public class MarketComparisonRepositoryImpl implements MarketComparisonRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void saveComparison(List<MarketComparisonEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (MarketComparisonEntity entity : entities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public void archiveComparison(List<MarketComparisonEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.market_comparison_history WHERE created_at = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.market_comparison_history WHERE created_at = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
        }
        for (MarketComparisonEntity entity : entities) {
            MarketComparisonHistoryEntity history = new MarketComparisonHistoryEntity();
            history.setSymbol(entity.getSymbol());
            history.setMarketRunId(entity.getMarketRunId());
            history.setMarketDate(entity.getMarketDate());
            history.setActualDate(entity.getActualDate());
            history.setPredictedOpen(entity.getPredictedOpen());
            history.setPredictedHigh(entity.getPredictedHigh());
            history.setPredictedLow(entity.getPredictedLow());
            history.setPredictedClose(entity.getPredictedClose());
            history.setPredictedDirection(entity.getPredictedDirection());
            history.setActualOpen(entity.getActualOpen());
            history.setActualHigh(entity.getActualHigh());
            history.setActualLow(entity.getActualLow());
            history.setActualClose(entity.getActualClose());
            history.setActualDirection(entity.getActualDirection());
            history.setCloseDiff(entity.getCloseDiff());
            history.setHighDiff(entity.getHighDiff());
            history.setLowDiff(entity.getLowDiff());
            history.setCloseDiffPercent(entity.getCloseDiffPercent());
            history.setHighDiffPercent(entity.getHighDiffPercent());
            history.setLowDiffPercent(entity.getLowDiffPercent());
            history.setDirectionMatched(entity.getDirectionMatched());
            history.setStatus(entity.getStatus());
            history.setReason(entity.getReason());
            history.setCreatedAt(entity.getCreatedAt());
            history.setPredictedProbabilityUp(entity.getPredictedProbabilityUp());
            history.setPredictedProbabilityDown(entity.getPredictedProbabilityDown());
            history.setConfidencePercent(entity.getConfidencePercent());
            history.setPredictedHighChangePct(entity.getPredictedHighChangePct());
            history.setPredictedLowChangePct(entity.getPredictedLowChangePct());
            history.setPredictedCloseChangePct(entity.getPredictedCloseChangePct());
            history.setActualVsPredictedDirectionMatch(entity.getActualVsPredictedDirectionMatch());
            history.setModelValidationAccuracyPercent(entity.getModelValidationAccuracyPercent());
            history.setRetrainingFeedbackScore(entity.getRetrainingFeedbackScore());
            entityManager.persist(history);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public void deleteByMarketDate() {
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.market_comparison").executeUpdate();
    }

    @Override
    public List<MarketComparisonEntity> findByMarketDate(LocalDate marketDate) {
        return entityManager.createQuery(
                        "SELECT m FROM MarketComparisonEntity m WHERE m.marketDate = :marketDate ORDER BY m.symbol",
                        MarketComparisonEntity.class)
                .setParameter("marketDate", marketDate)
                .getResultList();
    }

    @Override
    public List<MarketComparisonEntity> findBySymbolAndMarketDate(String symbol, LocalDate marketDate) {
        return entityManager.createQuery(
                        "SELECT m FROM MarketComparisonEntity m WHERE m.symbol = :symbol AND m.marketDate = :marketDate ORDER BY m.actualDate",
                        MarketComparisonEntity.class)
                .setParameter("symbol", symbol)
                .setParameter("marketDate", marketDate)
                .getResultList();
    }
}
