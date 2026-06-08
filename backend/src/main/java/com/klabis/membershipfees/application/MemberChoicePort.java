package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.util.Optional;

@PrimaryPort
public interface MemberChoicePort {

    record ChooseFeeLevel(MemberId memberId, MembershipFeeGroupId groupId, int year) {
        public ChooseFeeLevel {
            Assert.notNull(memberId, "MemberId is required");
            Assert.notNull(groupId, "MembershipFeeGroupId is required");
        }
    }

    void chooseFeeLevel(ChooseFeeLevel command);

    void removeFeeChoice(MemberId memberId, int year);

    Optional<MembershipFeeGroupId> getCurrentChoice(MemberId memberId, int year);

    Optional<MembershipFeeLevelId> getRecommendedLevelForYear(MemberId memberId, int year);
}
