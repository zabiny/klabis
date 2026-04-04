package com.klabis.members.traininggroup.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface TrainingGroupJdbcRepository extends CrudRepository<TrainingGroupMemento, UUID> {

    @Query("SELECT tg.* FROM training_groups tg " +
           "JOIN training_group_members tgm ON tg.id = tgm.training_group_id " +
           "WHERE tgm.member_id = :memberId LIMIT 1")
    Optional<TrainingGroupMemento> findByMemberId(@Param("memberId") UUID memberId);
}
