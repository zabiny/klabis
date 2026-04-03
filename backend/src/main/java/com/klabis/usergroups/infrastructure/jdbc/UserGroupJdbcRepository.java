package com.klabis.usergroups.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface UserGroupJdbcRepository extends CrudRepository<UserGroupMemento, UUID> {
}
