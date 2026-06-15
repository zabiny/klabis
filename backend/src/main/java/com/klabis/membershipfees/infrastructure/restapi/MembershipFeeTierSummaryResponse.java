package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.MembershipFeeTier;
import java.math.BigDecimal;
import java.util.UUID;

record MembershipFeeTierSummaryResponse(
        UUID id,
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        int ruleCount
) {
    static MembershipFeeTierSummaryResponse from(MembershipFeeTier level) {
        return new MembershipFeeTierSummaryResponse(
                level.getId().value(),
                level.getName(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode(),
                level.getRules().size()
        );
    }
}
