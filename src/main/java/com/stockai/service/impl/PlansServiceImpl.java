package com.stockai.service.impl;

import com.stockai.dto.PlanDto;
import com.stockai.entity.AuditLogEntity;
import com.stockai.entity.PlanEntity;
import com.stockai.entity.User;
import com.stockai.repository.AuditLogRepository;
import com.stockai.repository.PlansRepository;
import com.stockai.repository.UserRepository;
import com.stockai.service.interf.PlansService;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class PlansServiceImpl implements PlansService {

    @Autowired
    private PlansRepository plansRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;
    @Override
    public String addPlan(PlanDto planDto) {
        log.info("adding plan method execution started.");
        // Validate input
        if (planDto == null || planDto.getUserName() == null || planDto.getUserName().isBlank()) {
            throw new IllegalArgumentException("Plan data with userName is required");
        }

        // Ensure user exists
        Optional<User> userCheck = userRepository.findByEmailIgnoreCase(planDto.getUserName());
        if (userCheck.isEmpty()) {
            log.warn("Attempt to add/update plan for non-existing user: {}", planDto.getUserName());
            throw new IllegalArgumentException("User not found: " + planDto.getUserName());
        }

        PlanEntity existingPlan = plansRepository.findByUserName(planDto.getUserName());
        LocalDate currentDate = LocalDate.now();

        if (existingPlan == null) {
            PlanEntity entity = new PlanEntity();
            BeanUtils.copyProperties(planDto, entity);
            if (entity.getCreatedDate() == null) entity.setCreatedDate(LocalDateTime.now());
            if (entity.getUpdatedDate() == null) entity.setUpdatedDate(LocalDateTime.now());
            plansRepository.addPlan(entity);
            audit(planDto, planDto.getUserName(), "Plan_Added_Success_Fully");
            log.info("plan added for user {}", planDto.getUserName());
            return "Plan added successFully.";
        }
        LocalDate existingToDate = existingPlan.getToDate();
        if (existingToDate == null || existingToDate.isBefore(currentDate)) {
            PlanEntity entity = new PlanEntity();
            BeanUtils.copyProperties(planDto, entity);
            if (entity.getId() == null) entity.setId(existingPlan.getId());
            entity.setUpdatedDate(LocalDateTime.now());
            plansRepository.updatePlan(entity);
            audit(planDto, planDto.getUserName(), "Plan_Updated_Success_Fully");
            log.info("plan updated for user {}", planDto.getUserName());
            return "Plan updated successfully.";
        }
        log.info("plan available for user {}", planDto.getUserName());
        return "Plan_Available.";
    }

    private void audit(PlanDto user, String email, String eventType) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setUserId(user == null ? null : user.getId());
        entry.setEmail(email);
        entry.setEvent_type(eventType);
        entry.setEventAt(LocalDateTime.now());
        if (user != null) {
            if (user.getCreatedDate() != null) entry.setCreatedDate(user.getCreatedDate());
            if (user.getUpdatedDate() != null) entry.setUpdatedDate(user.getUpdatedDate().toLocalDate());
        }
        auditLogRepository.saveAuditLogEntity(entry);
    }
}
