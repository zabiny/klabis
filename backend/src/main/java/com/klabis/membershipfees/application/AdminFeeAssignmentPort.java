package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

@PrimaryPort
public interface AdminFeeAssignmentPort {

    record AssignFeeLevel(MemberId adminId, MemberId targetMemberId, MembershipFeeGroupId groupId, int year) {
        public AssignFeeLevel {
            Assert.notNull(adminId, "AdminId is required");
            Assert.notNull(targetMemberId, "TargetMemberId is required");
            Assert.notNull(groupId, "MembershipFeeGroupId is required");
        }
    }

    void assignLevel(AssignFeeLevel command);
}
