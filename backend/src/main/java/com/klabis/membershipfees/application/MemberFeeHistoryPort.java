package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@PrimaryPort
public interface MemberFeeHistoryPort {

    record CurrentLevelInfo(
            @Nullable MembershipFeeGroupId groupId,
            @Nullable String name,
            @Nullable Money yearlyFee,
            boolean votingOpen,
            Optional<MembershipFeeLevelId> recommendedLevelId
    ) {}

    record LevelAssignment(
            int year,
            MembershipFeeGroupId groupId,
            String groupName,
            LocalDate joinedAt,
            AssignmentSource source
    ) {}

    CurrentLevelInfo getCurrentLevelInfo(MemberId memberId, int year);

    List<LevelAssignment> getLevelHistory(MemberId memberId);
}
