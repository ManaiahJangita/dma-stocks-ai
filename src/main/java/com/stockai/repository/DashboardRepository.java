package com.stockai.repository;

import com.stockai.dto.DashboardFilterDto;
import com.stockai.entity.DashboardEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository {
	List<DashboardEntity> findAllDashboardData(DashboardFilterDto filters);

	void replaceDashboardData(List<DashboardEntity> rows);

	void deleteDashboardRecords();

	List<DashboardEntity> findAllDashboardSummary();

}
