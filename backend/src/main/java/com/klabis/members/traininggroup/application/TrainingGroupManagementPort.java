package com.klabis.members.traininggroup.application;

import com.klabis.members.MemberId;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface TrainingGroupManagementPort {

    TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command);

    TrainingGroup updateTrainingGroup(TrainingGroupId id, UpdateTrainingGroupCommand command);

    void deleteTrainingGroup(TrainingGroupId id);

    void addTrainer(TrainingGroupId id, MemberId trainerId);

    void removeTrainer(TrainingGroupId id, MemberId trainerId);

    void addMemberToTrainingGroup(TrainingGroupId id, MemberId memberId);

    void removeMemberFromTrainingGroup(TrainingGroupId id, MemberId memberId);
}
