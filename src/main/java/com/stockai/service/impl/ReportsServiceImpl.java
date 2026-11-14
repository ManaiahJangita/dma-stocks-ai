package com.stockai.service.impl;

import com.stockai.dto.ReportsDto;
import com.stockai.entity.ReportEntity;
import com.stockai.repository.ReportsRepository;
import com.stockai.service.interf.ReportsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
        try {
            List<ReportsDto> reports = new java.util.ArrayList<>();
            ReportEntity entityFilters = new ReportEntity();
            BeanUtils.copyProperties(filters == null ? new ReportsDto() : filters, entityFilters);
            List<ReportEntity> entities = reportsRepository.findAllReports(entityFilters);
            for (ReportEntity entity : entities) {
                ReportsDto dto = new ReportsDto();
                BeanUtils.copyProperties(entity, dto);
                reports.add(dto);
            }
            log.info("Report fetch completed with {} records", reports.size());
            return reports;
        } catch (Exception e) {
            log.error("Failed to fetch reports with filters: {}", filters, e);
            throw e;
        }
    }
}
