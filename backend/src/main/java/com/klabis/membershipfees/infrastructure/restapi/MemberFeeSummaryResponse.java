package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MemberFeeSummaryResponse(
        CurrentGroupResponse currentGroup,
        boolean votingOpen,
        UUID recommendedLevelId
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CurrentGroupResponse(UUID id, String name, BigDecimal yearlyFee) {}

    static MemberFeeSummaryResponse from(MemberFeeHistoryPort.CurrentLevelInfo info) {
        CurrentGroupResponse currentGroup = info.groupId() != null
                ? new CurrentGroupResponse(
                        info.groupId().value(),
                        info.name(),
                        info.yearlyFee().amount())
                : null;
        UUID recommendedLevelId = info.recommendedLevelId()
                .map(MembershipFeeLevelId::value)
                .orElse(null);
        return new MemberFeeSummaryResponse(currentGroup, info.votingOpen(), recommendedLevelId);
    }
}
