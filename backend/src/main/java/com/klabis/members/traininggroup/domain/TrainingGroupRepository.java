package com.klabis.members.traininggroup.domain;

import java.util.List;
import java.util.Optional;

public interface TrainingGroupRepository {

    TrainingGroup save(TrainingGroup group);

    Optional<TrainingGroup> findById(TrainingGroupId id);

    List<TrainingGroup> findAll();

    void delete(TrainingGroupId id);
}
