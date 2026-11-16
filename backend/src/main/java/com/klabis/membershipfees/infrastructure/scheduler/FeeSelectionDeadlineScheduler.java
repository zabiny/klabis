package com.klabis.membershipfees.infrastructure.scheduler;

import com.klabis.membershipfees.application.FeeYearLifecyclePort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.LocalDate;

@Service
class FeeSelectionDeadlineScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(FeeSelectionDeadlineScheduler.class);


    private final FeeYearLifecyclePort feeYearLifecyclePort;
    private final Clock clock;

    FeeSelectionDeadlineScheduler(FeeYearLifecyclePort feeYearLifecyclePort, Clock clock) {
        this.feeYearLifecyclePort = feeYearLifecyclePort;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void processMissedSelections() {
        LocalDate today = LocalDate.now(clock);
        feeYearLifecyclePort.processCampaignEnd(today);
    }

}
