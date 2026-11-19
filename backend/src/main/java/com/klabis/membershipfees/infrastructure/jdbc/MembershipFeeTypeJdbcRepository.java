package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

interface MembershipFeeTypeJdbcRepository extends CrudRepository<MembershipFeeTypeMemento, UUID> {

    @Override
    List<MembershipFeeTypeMemento> findAll();
}
