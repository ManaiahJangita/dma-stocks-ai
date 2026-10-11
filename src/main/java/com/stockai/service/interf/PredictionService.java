package com.stockai.service.interf;

import com.stockai.dto.PredictionDto;
import com.stockai.dto.PredictionFiltersDto;

import java.util.List;

public interface PredictionService {
    List<PredictionDto> getPredictionDetailsByFilters(PredictionFiltersDto filters);

    void synchronizePredictions(String startDate, String endDate);
}
