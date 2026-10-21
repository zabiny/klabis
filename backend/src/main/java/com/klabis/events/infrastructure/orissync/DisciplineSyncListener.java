package com.klabis.events.infrastructure.orissync;

import com.klabis.common.OrisIntegrationComponent;
import com.klabis.events.DisciplineArchivedEvent;
import com.klabis.events.DisciplineId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Self-listener on the {@code events} module's own domain events, mirroring {@link EventsSyncListener}
 * exactly: retires the {@code Discipline}'s {@code SyncRecord} once it reaches the end
 * of its life ({@link DisciplineArchivedEvent}) — a no-op if the discipline was never
 * paired to ORIS (manually created; {@link SynchronizationPort#findByTarget} then
 * returns empty).
 * <p>
 * A {@code @PrimaryAdapter} event listener is permitted to call a foreign module's
 * primary port ({@link SynchronizationPort}, in {@code sync.application}) — the same
 * pattern {@link EventsSyncListener} already uses. No {@code @Lazy} is needed here:
 * unlike {@link OrisEventDetailsReader}, this class is not itself collected by
 * {@code SynchronizationAdapterRegistry} (it is a plain event listener, not a
 * {@code SynchronizationAdapter} bean), so no circular bean dependency arises —
 * exactly the same reasoning that already lets {@code EventsSyncListener} depend on
 * {@link SynchronizationPort} directly.
 * <p>
 * {@link #reactivate} additionally provides the restore-side half of the sync engine's
 * existing retire/reactivate lifecycle.
 */
@OrisIntegrationComponent
@PrimaryAdapter
class DisciplineSyncListener {

    private final SynchronizationPort synchronizationPort;

    DisciplineSyncListener(SynchronizationPort synchronizationPort) {
        this.synchronizationPort = synchronizationPort;
    }

    @ApplicationModuleListener
    void handle(DisciplineArchivedEvent event) {
        synchronizationPort.findByTarget(targetFor(event.disciplineId()))
                .map(SyncRecord::getId)
                .ifPresent(synchronizationPort::retire);
    }

    /**
     * Reactivates a restored discipline's sync pairing: re-runs
     * {@link SynchronizationPort#pullAndEnroll} against the pairing's existing
     * {@code ExternalReference}, which reactivates a retired record — discarding its
     * stale baseline and starting over from ORIS's current values — exactly as {@code pullAndEnroll} already
     * does for any adapter's
     * retired pairing. A no-op when the discipline was never paired: {@code
     * findByTarget} then returns empty, and {@code Discipline.restore()}'s own
     * domain-only flag flip is all that happens.
     */
    void reactivate(DisciplineId disciplineId, String actingUser) {
        synchronizationPort.findByTarget(targetFor(disciplineId))
                .ifPresent(record -> synchronizationPort.pullAndEnroll(
                        SyncEntityType.DISCIPLINE, record.getExternalReference(), actingUser));
    }

    private static SyncTarget targetFor(DisciplineId disciplineId) {
        return new SyncTarget(SyncEntityType.DISCIPLINE, disciplineId.value().toString());
    }
}
