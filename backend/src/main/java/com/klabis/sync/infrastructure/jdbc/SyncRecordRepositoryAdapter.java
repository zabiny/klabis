package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncProjectionType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
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
    private final ObjectProvider<SyncProjectionType> projectionType;

    SyncRecordRepositoryAdapter(SyncRecordJdbcRepository jdbcRepository, ObjectProvider<SyncProjectionType> projectionType) {
        this.jdbcRepository = jdbcRepository;
        this.projectionType = projectionType;
    }

    @Override
    public SyncRecord save(SyncRecord record) {
        return jdbcRepository.save(SyncRecordMemento.from(record)).toSyncRecord(resolveProjectionType());
    }

    @Override
    public Optional<SyncRecord> findById(SyncRecordId id) {
        return jdbcRepository.findById(id.value()).map(memento -> memento.toSyncRecord(resolveProjectionType()));
    }

    @Override
    public Optional<SyncRecord> findByTargetAndSystem(SyncTarget target, ExternalSystem system) {
        return jdbcRepository.findByEntityTypeAndEntityIdAndExternalSystem(
                        target.entityType().name(), target.entityId(), system.name())
                .map(memento -> memento.toSyncRecord(resolveProjectionType()));
    }

    @Override
    public List<SyncRecord> findAllActive() {
        return jdbcRepository.findAllActive().stream()
                .map(memento -> memento.toSyncRecord(resolveProjectionType()))
                .toList();
    }

    @Override
    public List<SyncRecord> findDueForScan(Instant now, Duration claimLease) {
        return jdbcRepository.findDueForScan(now, now.minus(claimLease)).stream()
                .map(memento -> memento.toSyncRecord(resolveProjectionType()))
                .toList();
    }

    private SyncProjectionType resolveProjectionType() {
        return projectionType.getObject();
    }
}
