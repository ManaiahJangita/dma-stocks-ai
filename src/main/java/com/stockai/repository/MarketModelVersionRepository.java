package com.stockai.repository;

import com.stockai.entity.MarketModelVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketModelVersionRepository extends JpaRepository<MarketModelVersionEntity, UUID> {

    Optional<MarketModelVersionEntity> findTopByModelNameAndStatusOrderByCreatedAtDesc(String modelName, String status);
}

