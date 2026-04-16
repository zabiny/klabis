package com.klabis.members.groups.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupJdbcRepository extends CrudRepository<GroupMemento, UUID> {

    @Query("SELECT * FROM user_groups WHERE id = :id AND type = :type")
    Optional<GroupMemento> findByIdAndType(@Param("id") UUID id, @Param("type") String type);

    @Query("SELECT EXISTS (SELECT 1 FROM user_groups WHERE id = :id AND type = :type)")
    boolean existsByIdAndType(@Param("id") UUID id, @Param("type") String type);

    @Query("DELETE FROM user_groups WHERE id = :id AND type = :type")
    @Modifying
    void deleteByIdAndType(@Param("id") UUID id, @Param("type") String type);

    @Query("""
            SELECT ug.* FROM user_groups ug
            WHERE ug.type = :type
              AND (
                EXISTS (
                    SELECT 1 FROM user_group_owners ugo
                    WHERE ugo.user_group_id = ug.id AND ugo.member_id = :memberId
                ) OR EXISTS (
                    SELECT 1 FROM user_group_members ugm
                    WHERE ugm.user_group_id = ug.id AND ugm.member_id = :memberId
                )
              )
            """)
    List<GroupMemento> findOwnersOrMembersByType(@Param("memberId") UUID memberId, @Param("type") String type);

    @Query("""
            SELECT DISTINCT ug.* FROM user_groups ug
            JOIN user_group_invitations ugi ON ugi.user_group_id = ug.id
            WHERE ug.type = :type
              AND ugi.invited_member_id = :memberId
              AND ugi.status = 'PENDING'
            """)
    List<GroupMemento> findWithPendingInvitationsByType(@Param("memberId") UUID memberId, @Param("type") String type);

    @Query("""
            SELECT ug.* FROM user_groups ug
            JOIN user_group_members ugm ON ug.id = ugm.user_group_id
            WHERE ug.type = :type
              AND ugm.member_id = :memberId
            LIMIT 1
            """)
    Optional<GroupMemento> findByMemberIdAndType(@Param("memberId") UUID memberId, @Param("type") String type);

    @Query("""
            SELECT ug.* FROM user_groups ug
            JOIN user_group_owners ugo ON ug.id = ugo.user_group_id
            WHERE ug.type = :type
              AND ugo.member_id = :trainerId
            """)
    List<GroupMemento> findByTrainerIdAndType(@Param("trainerId") UUID trainerId, @Param("type") String type);

    @Query("""
            SELECT EXISTS (
                SELECT 1 FROM user_groups
                WHERE type = :type
                  AND (:excludeId IS NULL OR id != :excludeId)
                  AND age_range_min <= :maxAge
                  AND age_range_max >= :minAge
            )
            """)
    boolean existsOverlappingAgeRangeForType(@Param("minAge") int minAge, @Param("maxAge") int maxAge,
                                             @Param("excludeId") UUID excludeId,
                                             @Param("type") String type);

    @Query("SELECT * FROM user_groups WHERE type = :type")
    List<GroupMemento> findAllByType(@Param("type") String type);

    @Query("""
            SELECT ug.* FROM user_groups ug
            WHERE ug.type = :type
              AND (
                EXISTS (
                    SELECT 1 FROM user_group_owners ugo
                    WHERE ugo.user_group_id = ug.id AND ugo.member_id = :memberId
                ) OR EXISTS (
                    SELECT 1 FROM user_group_members ugm
                    WHERE ugm.user_group_id = ug.id AND ugm.member_id = :memberId
                )
              )
            LIMIT 1
            """)
    Optional<GroupMemento> findByMemberOrParentAndType(@Param("memberId") UUID memberId, @Param("type") String type);
}
