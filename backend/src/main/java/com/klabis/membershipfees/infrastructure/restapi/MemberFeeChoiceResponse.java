package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;

import java.util.Optional;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MemberFeeChoiceResponse(
        UUID memberId,
        int year,
        UUID currentGroupId,
        UUID recommendedLevelId
) {
    static MemberFeeChoiceResponse of(UUID memberId, int year,
                                       Optional<MembershipFeeGroupId> currentChoice,
                                       Optional<MembershipFeeLevelId> recommended) {
        return new MemberFeeChoiceResponse(
                memberId,
                year,
                currentChoice.map(MembershipFeeGroupId::value).orElse(null),
                recommended.map(MembershipFeeLevelId::value).orElse(null));
    }
}
