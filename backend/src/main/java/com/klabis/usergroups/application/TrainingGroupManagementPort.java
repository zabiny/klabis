package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.TrainingGroup;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface TrainingGroupManagementPort {

    TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command);

    TrainingGroup updateTrainingGroup(UserGroupId id, UpdateTrainingGroupCommand command);

    void deleteTrainingGroup(UserGroupId id);

    void addTrainer(UserGroupId id, MemberId trainerId);

    void removeTrainer(UserGroupId id, MemberId trainerId);

    void addMemberToTrainingGroup(UserGroupId id, MemberId memberId);

    void removeMemberFromTrainingGroup(UserGroupId id, MemberId memberId);
}
