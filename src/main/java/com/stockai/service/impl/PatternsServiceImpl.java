package com.stockai.service.impl;

import com.stockai.dto.PatternDto;
import com.stockai.dto.PatternFilterDto;
import com.stockai.entity.PatternEntity;
import com.stockai.repository.PatternsRepository;
import com.stockai.service.interf.PatternsService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
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
      List<PatternEntity> patterns = patternsRepository.findPatternByFilters(filters);
       return patterns.stream().map(pattern->{
           PatternDto patternDto = new PatternDto();
           BeanUtils.copyProperties(pattern, patternDto);
           return patternDto;
       }).collect(Collectors.toList());
    }
}
