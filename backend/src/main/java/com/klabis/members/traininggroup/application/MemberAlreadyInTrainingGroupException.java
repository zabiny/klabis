package com.klabis.members.traininggroup.application;

import com.klabis.members.MemberId;
import com.klabis.members.traininggroup.domain.TrainingGroupId;

public class MemberAlreadyInTrainingGroupException extends RuntimeException {

    public MemberAlreadyInTrainingGroupException(MemberId memberId, TrainingGroupId conflictingGroupId) {
        super("Member " + memberId + " is already a trainee of training group " + conflictingGroupId);
    }
}
