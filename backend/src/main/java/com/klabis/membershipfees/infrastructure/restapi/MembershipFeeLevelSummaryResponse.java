package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.MembershipFeeLevel;
import java.math.BigDecimal;
import java.util.UUID;

record MembershipFeeLevelSummaryResponse(
        UUID id,
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        int ruleCount
) {
    static MembershipFeeLevelSummaryResponse from(MembershipFeeLevel level) {
        return new MembershipFeeLevelSummaryResponse(
                level.getId().uuid(),
                level.getName(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode(),
                level.getRules().size()
        );
    }
}
