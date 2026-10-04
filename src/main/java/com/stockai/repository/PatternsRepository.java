package com.stockai.repository;

import com.stockai.entity.PatternEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatternsRepository {
	List<PatternEntity> findAllPatternData(PatternEntity filters);
}
