package com.klabis.members.traininggroup.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.members.traininggroup.domain.TrainingGroupId;

public class GroupNotFoundException extends ResourceNotFoundException {

    public GroupNotFoundException(TrainingGroupId id) {
        super("Training group not found with ID: " + id);
    }
}
