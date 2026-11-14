package com.stockai.service.impl;

import com.stockai.dto.PatternDto;
import com.stockai.dto.PatternFilterDto;
import com.stockai.entity.PatternEntity;
import com.stockai.repository.PatternsRepository;
import com.stockai.service.interf.PatternsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatternsServiceImpl implements PatternsService {
    @Autowired
    private PatternsRepository patternsRepository;

    @Override
    public List<PatternDto> fetchPatternsData(PatternFilterDto filters) {
        log.info("Fetching pattern data with filters: {}", filters);
        try {
            List<PatternEntity> patterns = patternsRepository.findPatternByFilters(filters);
            List<PatternDto> result = patterns.stream().map(pattern -> {
                PatternDto patternDto = new PatternDto();
                BeanUtils.copyProperties(pattern, patternDto);
                return patternDto;
            }).collect(Collectors.toList());
            log.info("Pattern data request completed with {} records", result.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch pattern data with filters: {}", filters, e);
            throw e;
        }
    }
}
