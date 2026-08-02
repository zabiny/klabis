package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.membershipfees.domain.MembershipPaymentRule;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
record PaymentRuleResponse(
        UUID eventTypeId,
        String rankingShortName,
        String ruleType,
        Integer percentage,
        BigDecimal fixedAmount,
        String fixedCurrency
) {
    static PaymentRuleResponse from(MembershipPaymentRule rule) {
        UUID eventTypeId = rule.eventTypeId().value();
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
