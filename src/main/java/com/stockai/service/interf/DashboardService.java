package com.stockai.service.interf;

import com.stockai.dto.DashboardDto;
import com.stockai.dto.DashboardFilterDto;
import com.stockai.dto.HistoricalDashboardFiltersDto;
import com.stockai.entity.HistoricalDashboardEntity;

import java.util.List;

public interface DashboardService {

    List<DashboardDto> fetchDashBoardData(DashboardFilterDto filters);

    List<DashboardDto> fetchDashBoardSummary();

    List<HistoricalDashboardEntity> fetchHistoricalDashboardData(HistoricalDashboardFiltersDto filters);

    int processDashboardData();
}
