package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

record EditPaymentRuleRequest(
        @NotBlank String ruleType,
        Integer percentage,
        BigDecimal fixedAmount,
        String fixedCurrency
) {
    MembershipPaymentRule.RuleValue toRuleValue(UUID eventTypeId, String rankingShortName) {
        return new PaymentRuleRequest(eventTypeId, rankingShortName, ruleType, percentage, fixedAmount, fixedCurrency)
                .toDomain()
                .value();
    }
}
