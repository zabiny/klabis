package com.klabis.members.legalguardian.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

interface LegalGuardianJdbcRepository extends CrudRepository<LegalGuardianMemento, UUID> {

    Optional<LegalGuardianMemento> findByEmailEqualsIgnoreCase(String email);
}
