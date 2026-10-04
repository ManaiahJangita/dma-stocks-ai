package com.stockai.service.impl;

import com.stockai.dto.DashboardDto;
import com.stockai.entity.DashboardEntity;
import com.stockai.repository.DashboardRepository;
import com.stockai.service.interf.DashboardService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Override
    public List<DashboardDto> fetchDashBoardData(DashboardDto filters) {
        List<DashboardDto> dashboardData = new ArrayList<>();
        DashboardEntity entityFilters = new DashboardEntity();
        BeanUtils.copyProperties(filters, entityFilters);
        for (DashboardEntity entity : dashboardRepository.findAllDashboardData(entityFilters)) {
            DashboardDto dto = new DashboardDto();
            BeanUtils.copyProperties(entity, dto);
            dashboardData.add(dto);
        }
        return dashboardData;
    }
}
