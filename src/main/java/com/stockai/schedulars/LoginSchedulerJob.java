package com.stockai.schedulars;

import com.stockai.data.feed.AngelOneRestService;
import com.stockai.schedulars.job.BasicJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoginSchedulerJob extends BasicJob {
    public static final String JOB_NAME = "LOGIN_SCHEDULER_JOB";

    @Autowired
    private AngelOneRestService angelOneRestService;
    @Scheduled(cron = "0 0 17 ? * MON-FRI")
    public void executeJob() {
        log.debug("Started login scheduler job");

        //logout the existing session
        angelOneRestService.logout();
        if (canProcess()) {
            //Generate new session
            angelOneRestService.login();
        }
        log.info("Ended login scheduler.");
    }
}

