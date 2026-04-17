package com.klabis.groups.traininggroup.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.members.MemberId;

public class MemberAlreadyInTrainingGroupException extends BusinessRuleViolationException {

    private final MemberId memberId;
    private final TrainingGroupId conflictingGroupId;

    public MemberAlreadyInTrainingGroupException(MemberId memberId, TrainingGroupId conflictingGroupId) {
        super("Member " + memberId + " is already a trainee of training group " + conflictingGroupId);
        this.memberId = memberId;
        this.conflictingGroupId = conflictingGroupId;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public TrainingGroupId getConflictingGroupId() {
        return conflictingGroupId;
    }
}
