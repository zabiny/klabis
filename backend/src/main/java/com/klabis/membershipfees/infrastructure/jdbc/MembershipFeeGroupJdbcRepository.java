package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface MembershipFeeGroupJdbcRepository extends CrudRepository<MembershipFeeGroupMemento, UUID> {

    @Query("SELECT * FROM membership_fee_group WHERE group_year = :year")
    List<MembershipFeeGroupMemento> findByYear(@Param("year") int year);
}
