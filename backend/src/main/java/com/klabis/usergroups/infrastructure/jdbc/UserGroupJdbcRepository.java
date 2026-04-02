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
           "JOIN user_group_owners ugo ON ug.id = ugo.user_group_id " +
           "WHERE ugo.member_id = :memberId")
    List<UserGroupMemento> findAllByOwnerId(@Param("memberId") UUID memberId);

    @Query("SELECT ug.* FROM user_groups ug " +
           "JOIN invitations inv ON ug.id = inv.group_id " +
           "WHERE inv.invited_member_id = :memberId AND inv.status = 'PENDING'")
    List<UserGroupMemento> findAllWithPendingInvitationForMember(@Param("memberId") UUID memberId);

    @Query("SELECT * FROM user_groups WHERE type = 'TRAINING'")
    List<UserGroupMemento> findAllTrainingGroups();

    @Query("SELECT * FROM user_groups WHERE type = 'FAMILY'")
    List<UserGroupMemento> findAllFamilyGroups();

    @Query("SELECT ug.* FROM user_groups ug " +
           "WHERE ug.type = 'FAMILY' AND (" +
           "EXISTS (SELECT 1 FROM user_group_members ugm WHERE ugm.user_group_id = ug.id AND ugm.member_id = :memberId) " +
           "OR EXISTS (SELECT 1 FROM user_group_owners ugo WHERE ugo.user_group_id = ug.id AND ugo.member_id = :memberId)" +
           ")")
    List<UserGroupMemento> findFamilyGroupsByMemberId(@Param("memberId") UUID memberId);
}
