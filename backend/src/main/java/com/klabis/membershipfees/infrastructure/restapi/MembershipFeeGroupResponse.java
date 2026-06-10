package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.MembershipFeeGroup;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record MembershipFeeGroupResponse(
        UUID id,
        UUID sourceLevelId,
        String name,
        int year,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        String status,
        int memberCount,
        List<MembershipFeeLevelResponse.PaymentRuleResponse> rulesSnapshot
) {
    static MembershipFeeGroupResponse from(MembershipFeeGroup group) {
        return new MembershipFeeGroupResponse(
                group.getId().value(),
                group.getSourceLevelId().value(),
                group.getName(),
                group.getYear(),
                group.getYearlyFeeSnapshot().amount(),
                group.getYearlyFeeSnapshot().currency().getCurrencyCode(),
                group.getStatus().name(),
                group.memberCount(),
                group.getRulesSnapshot().stream().map(MembershipFeeLevelResponse.PaymentRuleResponse::from).toList()
        );
    }
}
