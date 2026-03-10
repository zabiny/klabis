package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.BirthNumberAccessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Handles GDPR audit logging for birth number access and modification.
 *
 * <p>Runs in a separate transaction after the originating transaction commits,
 * so audit log failures never affect the primary operation.
 */
@Component
class BirthNumberAuditService {

    private static final Logger log = LoggerFactory.getLogger(BirthNumberAuditService.class);

    private final BirthNumberAuditLogJdbcRepository auditLogRepository;

    BirthNumberAuditService(BirthNumberAuditLogJdbcRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @ApplicationModuleListener
    void onBirthNumberAccessed(BirthNumberAccessedEvent event) {
        log.debug("Recording birth number audit: action={}, memberId={}, userId={}",
                event.action(), event.memberId(), event.actingUserId());

        BirthNumberAuditLogMemento entry = BirthNumberAuditLogMemento.of(
                event.actingUserId().uuid(),
                event.memberId().uuid(),
                event.action(),
                event.occurredAt()
        );
        auditLogRepository.save(entry);

        log.info("Birth number audit recorded: action={}, memberId={}", event.action(), event.memberId());
    }
}
