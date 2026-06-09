package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MembershipFeeGroupJdbcRepository extends CrudRepository<MembershipFeeGroupMemento, UUID> {

    @Query("SELECT * FROM membershipfees.membership_fee_group WHERE group_year = :year")
    List<MembershipFeeGroupMemento> findByYear(@Param("year") int year);

    @Query("""
            SELECT g.* FROM membershipfees.membership_fee_group g
            JOIN membershipfees.membership_fee_group_members m ON m.membership_fee_group_id = g.id
            WHERE m.member_id = :memberId AND g.group_year = :year
            LIMIT 1
            """)
    Optional<MembershipFeeGroupMemento> findByMemberAndYear(@Param("memberId") UUID memberId,
                                                             @Param("year") int year);

    @Query("""
            SELECT g.* FROM membershipfees.membership_fee_group g
            JOIN membershipfees.membership_fee_group_members m ON m.membership_fee_group_id = g.id
            WHERE m.member_id = :memberId
            ORDER BY g.group_year DESC
            """)
    List<MembershipFeeGroupMemento> findByMember(@Param("memberId") UUID memberId);

    @Query("SELECT COUNT(*) > 0 FROM membershipfees.membership_fee_group WHERE group_year = :year AND source_level_id = :sourceLevelId")
    boolean existsByYearAndSourceLevelId(@Param("year") int year, @Param("sourceLevelId") UUID sourceLevelId);
}
