package com.stockai.service.interf;

import com.stockai.dto.DashboardDto;

import java.util.List;

public interface DashboardService {

    List<DashboardDto> fetchDashBoardData(DashboardDto filters);
}
