package com.stockai.service.interf;

import com.stockai.dto.PatternDto;

import java.util.List;

public interface PatternsService {

    List<PatternDto> fetchPatternsData(PatternDto filters);
}
