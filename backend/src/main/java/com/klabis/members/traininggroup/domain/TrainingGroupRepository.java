package com.klabis.members.traininggroup.domain;

import com.klabis.members.groups.domain.TrainingGroupFilter;

import java.util.List;
import java.util.Optional;

public interface TrainingGroupRepository {

    TrainingGroup save(TrainingGroup group);

    Optional<TrainingGroup> findById(TrainingGroupId id);

    List<TrainingGroup> findAll(TrainingGroupFilter filter);

    Optional<TrainingGroup> findOne(TrainingGroupFilter filter);

    boolean exists(TrainingGroupFilter filter);

    void delete(TrainingGroupId id);
}
