package com.klabis.members.traininggroup.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TrainingGroupJdbcRepository extends CrudRepository<TrainingGroupMemento, UUID> {

    @Query("SELECT tg.* FROM training_groups tg " +
           "JOIN training_group_members tgm ON tg.id = tgm.training_group_id " +
           "WHERE tgm.member_id = :memberId LIMIT 1")
    Optional<TrainingGroupMemento> findByMemberId(@Param("memberId") UUID memberId);

    @Query("SELECT tg.* FROM training_groups tg " +
           "JOIN training_group_trainers tgt ON tg.id = tgt.training_group_id " +
           "WHERE tgt.member_id = :trainerId")
    List<TrainingGroupMemento> findByTrainerId(@Param("trainerId") UUID trainerId);

    @Query("SELECT EXISTS (" +
           "  SELECT 1 FROM training_groups " +
           "  WHERE (:excludeId IS NULL OR id != :excludeId) " +
           "    AND age_range_min <= :maxAge AND age_range_max >= :minAge" +
           ")")
    boolean existsOverlappingAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge,
                                      @Param("excludeId") UUID excludeId);
}
