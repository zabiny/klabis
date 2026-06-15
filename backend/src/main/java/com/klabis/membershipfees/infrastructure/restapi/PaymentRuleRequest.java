package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

record PaymentRuleRequest(
        @NotNull UUID eventTypeId,
        @NotBlank String rankingShortName,
        @NotBlank String ruleType,
        Integer percent,
        BigDecimal fixedAmount,
        String fixedCurrency
) {
    MembershipPaymentRule toDomain() {
        EventTypeReference evtTypeId = EventTypeReference.of(eventTypeId);
        return switch (ruleType) {
            case "PERCENTAGE" -> MembershipPaymentRule.percentage(evtTypeId, rankingShortName, percent);
            case "FIXED_AMOUNT" -> {
                String curr = fixedCurrency != null ? fixedCurrency : "CZK";
                yield MembershipPaymentRule.fixedAmount(evtTypeId, rankingShortName,
                        Money.of(fixedAmount, Currency.getInstance(curr)));
            }
            default -> throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        };
    }
}
