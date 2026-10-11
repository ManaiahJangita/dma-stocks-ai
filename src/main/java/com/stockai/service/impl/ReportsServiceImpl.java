package com.stockai.service.impl;

import com.stockai.dto.ReportsDto;
import com.stockai.dto.ReportDto;
import com.stockai.entity.ReportEntity;
import com.stockai.repository.ReportsRepository;
import com.stockai.service.interf.ReportsService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ReportsServiceImpl implements ReportsService {

    @Autowired
    private ReportsRepository reportsRepository;


    @Override
    public List<ReportsDto> fetchReportsBasedOnFilters(ReportsDto filters) {
        List<ReportsDto> reports = new java.util.ArrayList<>();
        ReportEntity entityFilters = new ReportEntity();
        BeanUtils.copyProperties(filters == null ? new ReportsDto() : filters, entityFilters);
        for (ReportEntity entity : reportsRepository.findAllReports(entityFilters)) {
            ReportsDto dto = new ReportsDto();
            BeanUtils.copyProperties(entity, dto);
            reports.add(dto);
        }
        return reports;
    }
}
