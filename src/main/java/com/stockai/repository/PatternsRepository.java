package com.stockai.repository;

import com.stockai.dto.PatternFilterDto;
import com.stockai.entity.HistoricalPatternEntity;
import com.stockai.entity.PatternEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatternsRepository {
	List<PatternEntity> findAllPatternData(PatternEntity filters);

	int replacePatterns(List<PatternEntity> rows);

	void truncatePatternRecords();

	void saveAllHistoricalPatterns(List<HistoricalPatternEntity> rows);

	List<PatternEntity> findPatternByFilters(PatternFilterDto filters);
}
