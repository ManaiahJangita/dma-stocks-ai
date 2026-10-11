package com.stockai.service.interf;

import com.stockai.dto.DashboardDto;
import com.stockai.dto.DashboardFilterDto;

import java.util.List;

public interface DashboardService {

    List<DashboardDto> fetchDashBoardData(DashboardFilterDto filters);

    List<DashboardDto> fetchDashBoardSummary();

    int processDashboardData();
}
