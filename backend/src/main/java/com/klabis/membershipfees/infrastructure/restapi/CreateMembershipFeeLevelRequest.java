package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

record CreateMembershipFeeLevelRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<PaymentRuleRequest> rules
) {
    MembershipFeeLevelManagementPort.CreateLevelCommand toCommand() {
        String currency = yearlyFeeCurrency != null ? yearlyFeeCurrency : "CZK";
        Money yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(currency));
        List<MembershipPaymentRule> domainRules = rules == null ? List.of()
                : rules.stream().map(PaymentRuleRequest::toDomain).toList();
        return new MembershipFeeLevelManagementPort.CreateLevelCommand(name, yearlyFee, domainRules);
    }

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
}
