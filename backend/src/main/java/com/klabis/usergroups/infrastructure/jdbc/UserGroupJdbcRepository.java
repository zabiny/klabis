package com.klabis.usergroups.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface UserGroupJdbcRepository extends CrudRepository<UserGroupMemento, UUID> {

    @Query("SELECT ug.* FROM user_groups ug " +
           "JOIN user_group_members ugm ON ug.id = ugm.user_group_id " +
           "WHERE ugm.member_id = :memberId")
    List<UserGroupMemento> findAllByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT ug.* FROM user_groups ug " +
           "JOIN invitations inv ON ug.id = inv.group_id " +
           "WHERE inv.invited_member_id = :memberId AND inv.status = 'PENDING'")
    List<UserGroupMemento> findAllWithPendingInvitationForMember(@Param("memberId") UUID memberId);
}
