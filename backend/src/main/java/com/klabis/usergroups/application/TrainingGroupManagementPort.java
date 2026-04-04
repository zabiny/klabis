package com.klabis.usergroups.application;

import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.TrainingGroup;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface TrainingGroupManagementPort {

    TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command);

    TrainingGroup updateTrainingGroup(UserGroupId id, UpdateTrainingGroupCommand command);

    void deleteTrainingGroup(UserGroupId id);
}
