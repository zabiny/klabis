package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncAttempt;
import com.klabis.sync.domain.SyncAttemptRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;

@SecondaryAdapter
@Repository
class SyncAttemptRepositoryAdapter implements SyncAttemptRepository {

    private final SyncAttemptJdbcRepository jdbcRepository;

    SyncAttemptRepositoryAdapter(SyncAttemptJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public SyncAttempt save(SyncAttempt attempt) {
        return jdbcRepository.save(SyncAttemptMemento.from(attempt)).toSyncAttempt();
    }

    @Override
    public List<SyncAttempt> findByRecordIdOrderByStartedAtDesc(SyncRecordId recordId) {
        return jdbcRepository.findBySyncRecordIdOrderByStartedAtDesc(recordId.value())
                .stream()
                .map(SyncAttemptMemento::toSyncAttempt)
                .toList();
    }
}
