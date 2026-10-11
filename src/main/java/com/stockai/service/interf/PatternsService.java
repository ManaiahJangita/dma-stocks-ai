package com.stockai.service.interf;

import com.stockai.dto.PatternDto;
import com.stockai.dto.PatternFilterDto;

import java.util.List;

public interface PatternsService {

    List<PatternDto> fetchPatternsData(PatternFilterDto filters);

}
