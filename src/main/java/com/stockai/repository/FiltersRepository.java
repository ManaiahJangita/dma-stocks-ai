package com.stockai.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiltersRepository {

    List<String> getReportFilters();

    List<String> getPatternFilters();

}
