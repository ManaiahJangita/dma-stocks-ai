package com.stockai.repository;

import com.stockai.entity.MarketModelWeightEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketModelWeightRepository extends JpaRepository<MarketModelWeightEntity, UUID> {

    List<MarketModelWeightEntity> findByModelVersionIdOrderBySignalName(UUID modelVersionId);
}

