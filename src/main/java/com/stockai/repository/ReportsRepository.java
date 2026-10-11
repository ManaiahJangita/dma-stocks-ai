package com.stockai.repository;

import com.stockai.entity.ReportEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportsRepository {
	List<ReportEntity> findAllReports(ReportEntity filters);

	int replaceReports(List<ReportEntity> rows);
}
