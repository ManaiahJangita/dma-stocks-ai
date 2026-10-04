package com.stockai.repository;

import com.stockai.entity.PlanEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlansRepository {
	void addPlan(PlanEntity planEntity);

	PlanEntity findByUserName(String name);

	PlanEntity updatePlan(PlanEntity planEntity);

	List<PlanEntity> findAllPlans();
}
