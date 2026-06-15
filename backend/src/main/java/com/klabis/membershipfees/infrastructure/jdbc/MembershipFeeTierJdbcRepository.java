package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

interface MembershipFeeTierJdbcRepository extends CrudRepository<MembershipFeeTierMemento, UUID> {

    @Override
    List<MembershipFeeTierMemento> findAll();
}
