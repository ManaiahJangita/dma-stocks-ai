package com.stockai.schedulars;

import com.stockai.entity.PlanEntity;
import com.stockai.entity.User;
import com.stockai.repository.PlansRepository;
import com.stockai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PlanStatusScheduler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlansRepository plansRepository;

    // runs every day at 00:10
    @Scheduled(cron = "0 10 0 * * ?")
    @Transactional
    public void checkAndUpdateUserPlanStatus() {
        log.info("PlanStatusScheduler: starting scheduled check of user plans");
        List<User> users = userRepository.findAll();
        if (users == null || users.isEmpty()) {
            log.info("PlanStatusScheduler: no users found");
            return;
        }

        LocalDate today = LocalDate.now();
        List<User> toUpdate = new ArrayList<>();

        for (User user : users) {
            try {
                PlanEntity plan = plansRepository.findByUserName(user.getEmail());
                boolean hasActivePlan = false;
                if (plan != null) {
                    boolean statusActive = plan.getStatus() != null && plan.getStatus().toLowerCase().contains("active");
                    boolean withinRange = (plan.getFromDate() == null || !plan.getFromDate().isAfter(today))
                            && (plan.getToDate() == null || !plan.getToDate().isBefore(today));
                    hasActivePlan = statusActive && withinRange;
                }

                if (!hasActivePlan) {
                    if (user.getStatus() == null || !user.getStatus().equalsIgnoreCase("INACTIVE")) {
                        user.setStatus("INACTIVE");
                        toUpdate.add(user);
                        log.debug("Marking user {} as INACTIVE (no active plan)", user.getEmail());
                    }
                } else {
                    if (user.getStatus() == null || user.getStatus().equalsIgnoreCase("INACTIVE")) {
                        user.setStatus("ACTIVE");
                        toUpdate.add(user);
                        log.debug("Marking user {} as ACTIVE (active plan found)", user.getEmail());
                    }
                }
            } catch (Exception ex) {
                log.error("Error while processing user {}: {}", user.getEmail(), ex.getMessage());
            }
        }

        if (!toUpdate.isEmpty()) {
            userRepository.saveAll(toUpdate);
            log.info("PlanStatusScheduler: updated {} users status", toUpdate.size());
        } else {
            log.info("PlanStatusScheduler: no user status changes needed");
        }
    }
}

