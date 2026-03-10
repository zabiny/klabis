package com.klabis.members.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface BirthNumberAuditLogJdbcRepository extends CrudRepository<BirthNumberAuditLogMemento, UUID> {
}
