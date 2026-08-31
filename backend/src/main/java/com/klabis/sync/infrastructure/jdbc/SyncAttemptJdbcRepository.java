package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface SyncAttemptJdbcRepository extends CrudRepository<SyncAttemptMemento, UUID> {

    List<SyncAttemptMemento> findBySyncRecordIdOrderByStartedAtDesc(UUID syncRecordId);
}
