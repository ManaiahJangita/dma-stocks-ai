package com.stockai.repository;

import com.stockai.entity.StatementEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatementRepository {
	List<StatementEntity> findAllStatements();

	StatementEntity saveStatement(StatementEntity statement);

	List<StatementEntity> findStatements(StatementEntity filters);
}
