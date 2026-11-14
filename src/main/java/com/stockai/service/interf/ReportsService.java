package com.stockai.service.interf;

import com.stockai.dto.ReportsDto;

import java.util.List;

public interface ReportsService {

    List<ReportsDto> fetchReportsBasedOnFilters(ReportsDto filters);
}
