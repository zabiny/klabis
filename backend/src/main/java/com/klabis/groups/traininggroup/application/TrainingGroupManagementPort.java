package com.klabis.groups.traininggroup.application;

import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface TrainingGroupManagementPort {

    List<TrainingGroup> listTrainingGroups();

    TrainingGroup getTrainingGroup(TrainingGroupId id);

    TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command);

    /**
     * The baseline {@link UpdateTrainingGroupCommand} for a group: every field pre-filled with the
     * current value. REST callers overlay only the fields their PATCH request changed, then pass the
     * result to {@link #updateTrainingGroup(TrainingGroupId, UpdateTrainingGroupCommand)}.
     */
    UpdateTrainingGroupCommand prefilledUpdateCommand(TrainingGroupId id);

    TrainingGroup updateTrainingGroup(TrainingGroupId id, UpdateTrainingGroupCommand command);

    void deleteTrainingGroup(TrainingGroupId id);

    void addTrainer(TrainingGroupId id, MemberId trainerId);

    void removeTrainer(TrainingGroupId id, MemberId trainerId);

    void addMemberToTrainingGroup(TrainingGroupId id, MemberId memberId);

    void removeMemberFromTrainingGroup(TrainingGroupId id, MemberId memberId);
}
