package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface SyncRecordJdbcRepository extends CrudRepository<SyncRecordMemento, UUID> {

    Optional<SyncRecordMemento> findByEntityTypeAndEntityIdAndExternalSystem(String entityType, String entityId, String externalSystem);
}
