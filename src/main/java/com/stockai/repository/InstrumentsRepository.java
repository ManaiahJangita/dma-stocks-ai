package com.stockai.repository;

import com.stockai.entity.InstrumentsEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstrumentsRepository {

    void saveAllInstrumetns(List<InstrumentsEntity> instrumentsEntities);

    @Query(value = "SELECT name FROM dmastock.instruments", nativeQuery = true)
    public List<String> findAllInstruments();

    public List<String> findInstrumentsSymbols();

    List<InstrumentsEntity> findInstrumentsByExchange(String exchange);

    @Query(value = "TRUNCATE TABLE dmastock.instruments", nativeQuery = true)
    public void truncateInstrumentsTable();
}
