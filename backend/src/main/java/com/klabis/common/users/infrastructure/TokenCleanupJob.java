package com.klabis.common.users.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to clean up expired password setup tokens.
 *
 * <p>Runs daily at midnight to delete expired tokens from the database.
 * This prevents the token table from growing indefinitely.
 */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final PasswordSetupTokenRepository tokenRepository;

    public TokenCleanupJob(PasswordSetupTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Scheduled cleanup task that runs daily at midnight.
     *
     * <p>Cron expression: 0 0 0 * * * (second minute hour day month day-of-week)
     * - Seconds: 0 (top of the minute)
     * - Minutes: 0 (top of the hour)
     * - Hour: 0 (midnight)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of the week)
     *
     * @return the number of deleted tokens
     */
    @Scheduled(cron = "0 0 0 * * *")
    public int cleanupExpiredTokens() {
        log.info("Starting password setup token cleanup job");

        try {
            int deletedCount = tokenRepository.deleteExpiredTokens();

            if (deletedCount > 0) {
                log.info("Token cleanup completed: {} expired tokens deleted", deletedCount);
            } else {
                log.info("Token cleanup completed: no expired tokens found");
            }

            return deletedCount;
        } catch (Exception e) {
            log.error("Error during token cleanup job: {}", e.getMessage(), e);
            return 0;
        }
    }
}
