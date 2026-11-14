package com.stockai.repository;

import com.stockai.entity.InstrumentsEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstrumentsRepository {

    void saveAllInstrumetns(List<InstrumentsEntity> instrumentsEntities);

    @Query(value = "SELECT name FROM dmastock.instruments", nativeQuery = true)
    List<String> findAllInstruments();

    List<String> findInstrumentsSymbols();

    @Query(value = "TRUNCATE TABLE dmastock.instruments", nativeQuery = true)
    void truncateInstrumentsTable();
}
