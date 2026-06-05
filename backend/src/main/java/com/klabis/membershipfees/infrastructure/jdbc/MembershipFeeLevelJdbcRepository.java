package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

interface MembershipFeeLevelJdbcRepository extends CrudRepository<MembershipFeeLevelMemento, UUID> {

    @Override
    List<MembershipFeeLevelMemento> findAll();
}
