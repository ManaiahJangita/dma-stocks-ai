package com.stockai.repository;

import com.stockai.entity.DashboardEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository {
	List<DashboardEntity> findAllDashboardData(DashboardEntity filters);
}
