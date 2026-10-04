package com.stockai.service.impl;

import com.stockai.dto.PatternDto;
import com.stockai.entity.PatternEntity;
import com.stockai.repository.PatternsRepository;
import com.stockai.service.interf.PatternsService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PatternsServiceImpl implements PatternsService {
    @Autowired
    private PatternsRepository patternsRepository;
    @Override
    public List<PatternDto> fetchPatternsData(PatternDto filters) {
        List<PatternDto> patterns = new java.util.ArrayList<>();
        PatternEntity entityFilters = new PatternEntity();
        BeanUtils.copyProperties(filters, entityFilters);
        for (PatternEntity entity : patternsRepository.findAllPatternData(entityFilters)) {
            PatternDto dto = new PatternDto();
            BeanUtils.copyProperties(entity, dto);
            patterns.add(dto);
        }
        return patterns;
    }
}
