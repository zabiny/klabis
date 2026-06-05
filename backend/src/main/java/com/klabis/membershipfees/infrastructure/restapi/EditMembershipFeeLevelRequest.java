package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.application.MembershipFeeLevelManagementPort;
import com.klabis.membershipfees.domain.MembershipPaymentRule;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

record EditMembershipFeeLevelRequest(
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<CreateMembershipFeeLevelRequest.PaymentRuleRequest> rules
) {
    MembershipFeeLevelManagementPort.EditLevelCommand toCommand() {
        Money yearlyFee = null;
        if (yearlyFeeAmount != null) {
            String currency = yearlyFeeCurrency != null ? yearlyFeeCurrency : "CZK";
            yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(currency));
        }
        List<MembershipPaymentRule> domainRules = rules == null ? null
                : rules.stream().map(CreateMembershipFeeLevelRequest.PaymentRuleRequest::toDomain).toList();
        return new MembershipFeeLevelManagementPort.EditLevelCommand(name, yearlyFee, domainRules);
    }
}
