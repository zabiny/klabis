package com.klabis.members.infrastructure.scheduler;

import com.klabis.members.application.MemberAgeOutPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

@Service
class MinorAgeOutJob {

    private static final Logger log = LoggerFactory.getLogger(MinorAgeOutJob.class);

    private final MemberAgeOutPort memberAgeOutPort;

    MinorAgeOutJob(MemberAgeOutPort memberAgeOutPort) {
        this.memberAgeOutPort = memberAgeOutPort;
    }

    @Scheduled(cron = "0 5 0 * * *")
    void processMembersComingOfAge() {
        processMembersComingOfAge(LocalDate.now());
    }

    void processMembersComingOfAge(LocalDate date) {
        log.info("Starting minor age-out job for date: {}", date);
        memberAgeOutPort.processMembersComingOfAge(date);
        log.info("Minor age-out job finished for date: {}", date);
    }
}
