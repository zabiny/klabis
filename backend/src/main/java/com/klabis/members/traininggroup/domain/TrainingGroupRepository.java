package com.klabis.members.traininggroup.domain;

import com.klabis.members.MemberId;

import java.util.List;
import java.util.Optional;

public interface TrainingGroupRepository {

    TrainingGroup save(TrainingGroup group);

    Optional<TrainingGroup> findById(TrainingGroupId id);

    Optional<TrainingGroup> findGroupForMember(MemberId memberId);

    List<TrainingGroup> findAll();

    void delete(TrainingGroupId id);
}
