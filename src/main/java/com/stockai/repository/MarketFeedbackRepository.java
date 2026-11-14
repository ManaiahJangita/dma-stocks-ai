package com.stockai.repository;

import com.stockai.entity.MarketFeedbackEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarketFeedbackRepository extends JpaRepository<MarketFeedbackEntity, UUID> {

    List<MarketFeedbackEntity> findByProcessedFalseOrderByCreatedAtAsc();

    List<MarketFeedbackEntity> findByProcessedTrueOrderByProcessedAtAsc();

    List<MarketFeedbackEntity> findByProcessedFalseAndDirectionMatchedFalseOrderByCreatedAtAsc();

    @Modifying
    @Transactional
    @Query("UPDATE MarketFeedbackEntity f SET f.processed = true, f.processedAt = :processedAt WHERE f.id IN :ids")
    void markProcessed(@Param("ids") List<UUID> ids, @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Transactional
    @Query("UPDATE MarketFeedbackEntity f SET f.processed = true, f.processedAt = :processedAt WHERE f.marketDate = :marketDate")
    void markProcessedByMarketDate(@Param("marketDate") LocalDate marketDate, @Param("processedAt") LocalDateTime processedAt);

    @Modifying
    @Transactional
    void deleteByProcessedTrueAndProcessedAtBefore(LocalDateTime cutoff);
}
