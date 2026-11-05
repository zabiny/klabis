package com.klabis.groups.traininggroup.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.members.MemberId;

import java.util.Map;

public class MemberAlreadyInTrainingGroupException extends ConflictException implements UserFacingError {

    private final MemberId memberId;
    private final TrainingGroupId conflictingGroupId;
    private final String conflictingGroupName;

    public MemberAlreadyInTrainingGroupException(MemberId memberId, TrainingGroupId conflictingGroupId,
                                                 String conflictingGroupName) {
        super("Member " + memberId + " is already a trainee of training group " + conflictingGroupId);
        this.memberId = memberId;
        this.conflictingGroupId = conflictingGroupId;
        this.conflictingGroupName = conflictingGroupName;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    public TrainingGroupId getConflictingGroupId() {
        return conflictingGroupId;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("groupName", conflictingGroupName);
    }

    @Override
    public String field() {
        return "memberId";
    }
}
