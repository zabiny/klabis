package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.MembershipPaymentRule;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

record EditMembershipFeeTierRequest(
        String name,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<CreateMembershipFeeTierRequest.PaymentRuleRequest> rules
) {
    MembershipFeeTierManagementPort.EditTierCommand toCommand() {
        Money yearlyFee = null;
        if (yearlyFeeAmount != null) {
            String currency = yearlyFeeCurrency != null ? yearlyFeeCurrency : "CZK";
            yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(currency));
        }
        List<MembershipPaymentRule> domainRules = rules == null ? null
                : rules.stream().map(CreateMembershipFeeTierRequest.PaymentRuleRequest::toDomain).toList();
        return new MembershipFeeTierManagementPort.EditTierCommand(name, yearlyFee, domainRules);
    }
}
