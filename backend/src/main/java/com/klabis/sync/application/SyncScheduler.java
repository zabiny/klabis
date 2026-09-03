package com.klabis.sync.application;

import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Drives the two scheduled scan cadences (design.md D10, D19): a nightly full pass
 * that re-compares every active record — the only way an external change that
 * announces itself nowhere gets noticed — and a frequent due scan that only touches
 * dirty or retry-due records via one indexed query.
 * <p>
 * Both cadences run each due record through {@link SynchronizationService#runScheduledPass},
 * not {@link SynchronizationService#synchronizeNow} — a scheduled pass re-evaluates a
 * standing {@code CONFLICT} and can clear it (design.md D7), which the manual trigger
 * explicitly refuses.
 * <p>
 * Before each record, the scan consults {@link ResilientAdapterExecutor#isOpen()}: an
 * open circuit breaker means the external system is down, so the scan stops and
 * leaves the remaining records untouched rather than each attempting, failing and
 * consuming its retry budget (design.md D11).
 */
@Component
class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncRecordRepository syncRecordRepository;
    private final SynchronizationService synchronizationService;
    private final ResilientAdapterExecutor resilientAdapterExecutor;
    private final SyncProperties properties;

    SyncScheduler(
            SyncRecordRepository syncRecordRepository,
            SynchronizationService synchronizationService,
            ResilientAdapterExecutor resilientAdapterExecutor,
            SyncProperties properties
    ) {
        this.syncRecordRepository = syncRecordRepository;
        this.synchronizationService = synchronizationService;
        this.resilientAdapterExecutor = resilientAdapterExecutor;
        this.properties = properties;
    }

    @Scheduled(cron = "${klabis.sync.scan-cron}")
    void runFullPass() {
        log.info("Starting sync full pass");
        runScanOver(syncRecordRepository.findAllActive());
    }

    @Scheduled(fixedRateString = "${klabis.sync.due-scan-interval}")
    void runDueScan() {
        log.info("Starting sync due scan");
        runScanOver(syncRecordRepository.findDueForScan(Instant.now(), properties.getClaimLease()));
    }

    private void runScanOver(List<SyncRecord> candidates) {
        int processed = 0;
        for (SyncRecord candidate : candidates) {
            if (resilientAdapterExecutor.isOpen()) {
                log.warn("sync-adapter circuit breaker is open, stopping scan with {} of {} records left untouched",
                        candidates.size() - processed, candidates.size());
                return;
            }
            try {
                synchronizationService.runScheduledPass(candidate.getId());
            } catch (SyncRecordClaimedException e) {
                // Expected race: a concurrent pass (manual or another scheduled run)
                // claimed this record first — not a failure worth alarming on.
                log.debug("Sync record {} was claimed by a concurrent pass, skipping", candidate.getId());
            } catch (RuntimeException e) {
                log.error("Scheduled sync pass failed for record {}: {}", candidate.getId(), e.getMessage(), e);
            }
            processed++;
        }
        log.info("Sync scan completed, {} record(s) processed", processed);
    }
}
