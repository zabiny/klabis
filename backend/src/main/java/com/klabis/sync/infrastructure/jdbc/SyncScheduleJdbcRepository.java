package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface SyncScheduleJdbcRepository extends CrudRepository<SyncScheduleMemento, UUID> {
}
