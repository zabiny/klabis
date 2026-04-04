package com.klabis.members.membersgroup.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface MembersGroupJdbcRepository extends CrudRepository<MembersGroupMemento, UUID> {

    @Query("""
            SELECT mg.* FROM members_groups mg
            WHERE EXISTS (
                SELECT 1 FROM members_group_owners mgo
                WHERE mgo.members_group_id = mg.id AND mgo.member_id = :memberId
            ) OR EXISTS (
                SELECT 1 FROM members_group_members mgm
                WHERE mgm.members_group_id = mg.id AND mgm.member_id = :memberId
            )
            """)
    List<MembersGroupMemento> findGroupsForMember(@Param("memberId") UUID memberId);

    @Query("""
            SELECT DISTINCT mg.* FROM members_groups mg
            JOIN members_group_invitations mgi ON mgi.members_group_id = mg.id
            WHERE mgi.invited_member_id = :memberId AND mgi.status = 'PENDING'
            """)
    List<MembersGroupMemento> findGroupsWithPendingInvitationsForMember(@Param("memberId") UUID memberId);
}
