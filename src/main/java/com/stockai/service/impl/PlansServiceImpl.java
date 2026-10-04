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
        Optional<User> userCheck = userRepository.findByEmailIgnoreCase(planDto.getUserName());
        PlanEntity planCheck = plansRepository.findByUserName(planDto.getUserName());
        LocalDate planDate = Objects.nonNull(planCheck)?planCheck.getToDate():null;
        LocalDate currentDate = LocalDate.now();
        if(Objects.nonNull(userCheck) && planDate == null){
            PlanEntity entity = new PlanEntity();
            BeanUtils.copyProperties(planDto,entity);
            plansRepository.addPlan(entity);
            audit(planDto,planDto.getUserName(),"Plan_Added_Success_Fully");
            log.info("plan added");
         return "Plan added successFully.";
        }else if(planDate != null && planDate.isBefore(currentDate)){
            PlanEntity entity = new PlanEntity();
            BeanUtils.copyProperties(planDto, entity);
            plansRepository.updatePlan(entity);
            audit(planDto,planDto.getUserName(),"Plan_Updated_Success_Fully");
            log.info("plan updated");
            return "Plan updated successfully.";
        }else{
            log.info("plan avaialable shwos");
            return "Plan_Available.";
        }
    }

    private void audit(PlanDto user, String email, String eventType) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setUserId(user == null ? null : user.getId());
        entry.setEmail(email);
        entry.setEvent_type(eventType);
        entry.setEventAt(LocalDateTime.now());
        entry.setCreatedDate(user.getCreatedDate());
        entry.setUpdatedDate(user.getUpdatedDate().toLocalDate());
        auditLogRepository.saveAuditLogEntity(entry);
    }
}
