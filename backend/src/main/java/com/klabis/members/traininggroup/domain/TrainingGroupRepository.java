package com.klabis.members.traininggroup.domain;

import com.klabis.members.MemberId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingGroupRepository {

    TrainingGroup save(TrainingGroup group);

    Optional<TrainingGroup> findById(TrainingGroupId id);

    Optional<TrainingGroup> findGroupForMember(MemberId memberId);

    List<TrainingGroup> findGroupsForTrainer(MemberId trainerId);

    List<TrainingGroup> findAll();

    boolean existsOverlappingAgeRange(int minAge, int maxAge, TrainingGroupId excludeId);

    void delete(TrainingGroupId id);
}
