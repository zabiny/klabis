package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.MembershipFeeTier;

import java.math.BigDecimal;
import java.util.UUID;

record MembershipFeeTierResponse(
        UUID id,
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency
) {
    static MembershipFeeTierResponse from(MembershipFeeTier level) {
        return new MembershipFeeTierResponse(
                level.getId().value(),
                level.getName(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode()
        );
    }
}
