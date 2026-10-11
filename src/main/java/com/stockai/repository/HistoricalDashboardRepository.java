package com.stockai.repository;

import com.stockai.entity.HistoricalDashboardEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricalDashboardRepository {
    void saveAllHistoricalDashboards(List<HistoricalDashboardEntity> entities);
}
