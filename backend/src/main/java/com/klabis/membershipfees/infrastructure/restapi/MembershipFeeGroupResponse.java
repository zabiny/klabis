package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import com.klabis.membershipfees.domain.MembershipPaymentRuleSnapshot;

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
        List<RuleSnapshotResponse> rulesSnapshot
) {
    static MembershipFeeGroupResponse from(MembershipFeeGroup group) {
        return new MembershipFeeGroupResponse(
                group.getId().uuid(),
                group.getSourceLevelId().value(),
                group.getName(),
                group.getYear(),
                group.getYearlyFeeSnapshot().amount(),
                group.getYearlyFeeSnapshot().currency().getCurrencyCode(),
                group.getStatus().name(),
                group.memberCount(),
                group.getRulesSnapshot().stream().map(RuleSnapshotResponse::from).toList()
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RuleSnapshotResponse(
            UUID eventTypeId,
            String rankingShortName,
            String ruleType,
            Integer percent,
            BigDecimal fixedAmount,
            String fixedCurrency
    ) {
        static RuleSnapshotResponse from(MembershipPaymentRuleSnapshot snapshot) {
            return switch (snapshot.value()) {
                case MembershipPaymentRule.RuleValue.Percentage p ->
                        new RuleSnapshotResponse(snapshot.eventTypeId().value(), snapshot.rankingShortName(),
                                "PERCENTAGE", p.percent(), null, null);
                case MembershipPaymentRule.RuleValue.FixedSurcharge f ->
                        new RuleSnapshotResponse(snapshot.eventTypeId().value(), snapshot.rankingShortName(),
                                "FIXED_SURCHARGE", null, f.amount().amount(),
                                f.amount().currency().getCurrencyCode());
            };
        }
    }
}
