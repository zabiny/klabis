package com.klabis.events.infrastructure.scheduler;

import com.klabis.events.application.EventManagementPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

/**
 * Scheduled job for automatically completing events after their event date has passed.
 * <p>
 * Runs daily at 2:00 AM and delegates to {@link EventManagementPort} to transition
 * ACTIVE events with past dates to FINISHED status.
 */
@Service
class EventCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventCompletionScheduler.class);

    private final EventManagementPort eventManagementPort;

    EventCompletionScheduler(EventManagementPort eventManagementPort) {
        this.eventManagementPort = eventManagementPort;
    }

    @Scheduled(cron = "0 0 2 * * *")
    void completeExpiredEvents() {
        completeExpiredEvents(LocalDate.now());
    }

    void completeExpiredEvents(LocalDate date) {
        log.info("Starting event completion scheduler for date: {}", date);
        eventManagementPort.finishExpiredActiveEvents(date);
        log.info("Event completion scheduler finished for date: {}", date);
    }
}
