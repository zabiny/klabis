package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.MembershipPaymentRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

record AddPaymentRuleRequest(
        @NotNull UUID eventTypeId,
        @NotBlank String rankingShortName,
        @NotBlank String ruleType,
        Integer percentage,
        BigDecimal fixedAmount,
        String fixedCurrency
) {
    MembershipPaymentRule toDomain() {
        return new PaymentRuleRequest(eventTypeId, rankingShortName, ruleType, percentage, fixedAmount, fixedCurrency)
                .toDomain();
    }
}
