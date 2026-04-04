package com.klabis.members.traininggroup.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

interface TrainingGroupJdbcRepository extends CrudRepository<TrainingGroupMemento, UUID> {
}
