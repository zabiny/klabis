package com.klabis.sync.application;

import com.klabis.sync.domain.SyncAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Prunes {@code sync_attempt} rows older than {@code history-retention} (design.md
 * D19). Never deletes {@code sync_record} rows — the record's own
 * {@code last_successful_sync_at} carries the information a caller needs after its
 * attempt history has been pruned. Modeled on
 * {@link com.klabis.common.users.infrastructure.TokenCleanupJob}.
 */
@Component
class SyncHistoryRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(SyncHistoryRetentionJob.class);

    private final SyncAttemptRepository syncAttemptRepository;
    private final SyncProperties properties;

    SyncHistoryRetentionJob(SyncAttemptRepository syncAttemptRepository, SyncProperties properties) {
        this.syncAttemptRepository = syncAttemptRepository;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    int cleanupExpiredAttempts() {
        log.info("Starting sync attempt history retention cleanup");
        try {
            Instant olderThan = Instant.now().minus(properties.getHistoryRetention());
            int deletedCount = syncAttemptRepository.deleteAttemptsStartedBefore(olderThan);
            if (deletedCount > 0) {
                log.info("Sync attempt history cleanup completed: {} attempt(s) deleted", deletedCount);
            } else {
                log.info("Sync attempt history cleanup completed: no expired attempts found");
            }
            return deletedCount;
        } catch (Exception e) {
            log.error("Error during sync attempt history cleanup: {}", e.getMessage(), e);
            return 0;
        }
    }
}
