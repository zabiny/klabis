package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipPaymentRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record MembershipFeeTierResponse(
        UUID id,
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<PaymentRuleResponse> rules
) {
    static MembershipFeeTierResponse from(MembershipFeeTier level) {
        List<PaymentRuleResponse> ruleResponses = level.getRules().stream()
                .map(PaymentRuleResponse::from)
                .toList();
        return new MembershipFeeTierResponse(
                level.getId().value(),
                level.getName(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode(),
                ruleResponses
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaymentRuleResponse(
            EventTypeReference eventTypeId,
            String rankingShortName,
            String ruleType,
            Integer percent,
            BigDecimal fixedAmount,
            String fixedCurrency
    ) {
        static PaymentRuleResponse from(MembershipPaymentRule rule) {
            var eventTypeId = rule.eventTypeId();
            return switch (rule.value()) {
                case MembershipPaymentRule.RuleValue.Percentage p ->
                        new PaymentRuleResponse(eventTypeId, rule.rankingShortName(),
                                "PERCENTAGE", p.percent(), null, null);
                case MembershipPaymentRule.RuleValue.FixedAmount f ->
                        new PaymentRuleResponse(eventTypeId, rule.rankingShortName(),
                                "FIXED_AMOUNT", null, f.amount().amount(),
                                f.amount().currency().getCurrencyCode());
            };
        }
    }
}
