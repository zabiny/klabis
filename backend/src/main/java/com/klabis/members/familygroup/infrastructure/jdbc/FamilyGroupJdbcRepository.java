package com.klabis.members.familygroup.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface FamilyGroupJdbcRepository extends CrudRepository<FamilyGroupMemento, UUID> {

    @Query("""
            SELECT fg.* FROM family_groups fg
            WHERE EXISTS (
                SELECT 1 FROM family_group_parents fgp
                WHERE fgp.family_group_id = fg.id AND fgp.member_id = :memberId
            ) OR EXISTS (
                SELECT 1 FROM family_group_children fgc
                WHERE fgc.family_group_id = fg.id AND fgc.member_id = :memberId
            )
            LIMIT 1
            """)
    Optional<FamilyGroupMemento> findByMemberOrParent(@Param("memberId") UUID memberId);
}
