package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncProjectionType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncSchedule;
import com.klabis.sync.domain.SyncScheduleRepository;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * {@code SyncProjectionType} is resolved lazily via {@link ObjectProvider} rather than
 * a constructor dependency: this class is picked up by every {@code @DataJdbcTest}
 * slice across the codebase (they scan for {@code @Repository}-annotated beans), most
 * of which never register a {@code SyncProjectionType} bean and would otherwise fail
 * to start their context even though they never touch synchronisation.
 */
@SecondaryAdapter
@Repository
class SyncRecordRepositoryAdapter implements SyncRecordRepository {

    private final SyncRecordJdbcRepository jdbcRepository;
    private final SyncScheduleRepository scheduleRepository;
    private final ObjectProvider<SyncProjectionType> projectionType;

    SyncRecordRepositoryAdapter(SyncRecordJdbcRepository jdbcRepository, SyncScheduleRepository scheduleRepository, ObjectProvider<SyncProjectionType> projectionType) {
        this.jdbcRepository = jdbcRepository;
        this.scheduleRepository = scheduleRepository;
        this.projectionType = projectionType;
    }

    @Override
    public SyncRecord save(SyncRecord record) {
        SyncRecordMemento saved = jdbcRepository.save(SyncRecordMemento.from(record));
        return saved.toSyncRecord(resolveProjectionType(), loadSchedule(saved));
    }

    @Override
    public Optional<SyncRecord> findById(SyncRecordId id) {
        return jdbcRepository.findById(id.value()).map(this::toSyncRecord);
    }

    @Override
    public Optional<SyncRecord> findByTargetAndSystem(SyncTarget target, ExternalSystem system) {
        return jdbcRepository.findByEntityTypeAndEntityIdAndExternalSystem(
                        target.entityType().name(), target.entityId(), system.name())
                .map(this::toSyncRecord);
    }

    @Override
    public Optional<SyncRecord> findBySystemAndExternalId(ExternalSystem system, String externalId) {
        return jdbcRepository.findByExternalSystemAndExternalId(system.name(), externalId)
                .map(this::toSyncRecord);
    }

    @Override
    public List<SyncRecord> findAllActive() {
        return jdbcRepository.findAllActive().stream()
                .map(this::toSyncRecord)
                .toList();
    }

    @Override
    public List<SyncRecord> findAllNonRetired() {
        return jdbcRepository.findAllNonRetired().stream()
                .map(this::toSyncRecord)
                .toList();
    }

    @Override
    public List<SyncRecord> findDueForScan(Instant now, Duration claimLease) {
        return jdbcRepository.findDueForScan(now, now.minus(claimLease)).stream()
                .map(this::toSyncRecord)
                .toList();
    }

    private SyncRecord toSyncRecord(SyncRecordMemento memento) {
        return memento.toSyncRecord(resolveProjectionType(), loadSchedule(memento));
    }

    /**
     * Loaded eagerly, right alongside the record, for every read path (proposal.md
     * task 3.5's "never lazily" property, carried over from task 4b.2) — a second
     * query per record, not a single joined one, since {@link SyncScheduleRepository}
     * is deliberately its own port with its own read/write path (see its javadoc).
     * <p>
     * A deliberate N+1 on every list path ({@link #findAllActive}, {@link
     * #findAllNonRetired}, {@link #findDueForScan}), accepted rather than fixed
     * (simplify review): N is the count of active records — tens to low hundreds in
     * this deployment — and the nightly full pass that calls {@link #findAllActive}
     * runs once a day, already issuing at least four loads plus an HTTP call to ORIS
     * per record it attempts, so one more query per record is noise by comparison. The
     * due scan ({@link #findDueForScan}) typically returns zero records, so N there is
     * usually 0 anyway. A join would pull the schedule columns into {@code
     * SyncRecordMemento} and blur the boundary this change deliberately drew between
     * {@code sync_record} and {@code sync_schedule}. If the active-record count grows
     * into the thousands, join {@code sync_schedule} directly into {@link
     * SyncRecordJdbcRepository#findDueForScan}/{@link
     * SyncRecordJdbcRepository#findAllActive} — both already reference the table for
     * their own predicates.
     */
    private SyncSchedule loadSchedule(SyncRecordMemento memento) {
        return scheduleRepository.findByRecordId(new SyncRecordId(memento.getId()));
    }

    private SyncProjectionType resolveProjectionType() {
        return projectionType.getObject();
    }
}
