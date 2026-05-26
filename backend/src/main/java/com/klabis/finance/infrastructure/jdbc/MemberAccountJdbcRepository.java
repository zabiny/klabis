package com.klabis.finance.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface MemberAccountJdbcRepository extends CrudRepository<MemberAccountMemento, UUID> {
}
