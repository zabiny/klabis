package com.klabis.groups.traininggroup.domain;

import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.TrainingGroup;

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
