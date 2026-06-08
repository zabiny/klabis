package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MembershipFeeGroupJdbcRepository extends CrudRepository<MembershipFeeGroupMemento, UUID> {

    @Query("SELECT * FROM membership_fee_group WHERE group_year = :year")
    List<MembershipFeeGroupMemento> findByYear(@Param("year") int year);

    @Query("""
            SELECT g.* FROM membership_fee_group g
            JOIN fee_group_membership m ON m.membership_fee_group_id = g.id
            WHERE m.member_id = :memberId AND g.group_year = :year
            LIMIT 1
            """)
    Optional<MembershipFeeGroupMemento> findByMemberAndYear(@Param("memberId") UUID memberId,
                                                             @Param("year") int year);
}
