package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import com.klabis.membershipfees.domain.MembershipPaymentRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record MembershipFeeLevelResponse(
        UUID id,
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<PaymentRuleResponse> rules
) {
    static MembershipFeeLevelResponse from(MembershipFeeLevel level) {
        List<PaymentRuleResponse> ruleResponses = level.getRules().stream()
                .map(PaymentRuleResponse::from)
                .toList();
        return new MembershipFeeLevelResponse(
                level.getId().value(),
                level.getName(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode(),
                ruleResponses
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaymentRuleResponse(
            UUID eventTypeId,
            String rankingShortName,
            String ruleType,
            Integer percent,
            BigDecimal fixedAmount,
            String fixedCurrency
    ) {
        static PaymentRuleResponse from(MembershipPaymentRule rule) {
            return switch (rule.value()) {
                case MembershipPaymentRule.RuleValue.Percentage p ->
                        new PaymentRuleResponse(rule.eventTypeId().value(), rule.rankingShortName(),
                                "PERCENTAGE", p.percent(), null, null);
                case MembershipPaymentRule.RuleValue.FixedSurcharge f ->
                        new PaymentRuleResponse(rule.eventTypeId().value(), rule.rankingShortName(),
                                "FIXED_SURCHARGE", null, f.amount().amount(),
                                f.amount().currency().getCurrencyCode());
            };
        }
    }
}
