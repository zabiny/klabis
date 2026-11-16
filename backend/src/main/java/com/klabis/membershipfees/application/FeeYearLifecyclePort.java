package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.time.LocalDate;

@PrimaryPort
public interface FeeYearLifecyclePort {

    record AssignFeeLevel(MemberId adminId, MemberId targetMemberId, MembershipFeeGroupId groupId, int year) {
        public AssignFeeLevel {
            Assert.notNull(adminId, "AdminId is required");
            Assert.notNull(targetMemberId, "TargetMemberId is required");
            Assert.notNull(groupId, "MembershipFeeGroupId is required");
        }
    }

    record ChooseFeeLevel(MemberId memberId, MembershipFeeGroupId groupId, int year) {
        public ChooseFeeLevel {
            Assert.notNull(memberId, "MemberId is required");
            Assert.notNull(groupId, "MembershipFeeGroupId is required");
        }
    }

    void closeCampaign(FeeSelectionCampaignId campaignId);

    void processCampaignEnd(LocalDate toDate);

    void assignLevel(AssignFeeLevel command);

    void chooseFeeLevel(ChooseFeeLevel command);

    void removeFeeChoice(MemberId memberId, int year);
}
