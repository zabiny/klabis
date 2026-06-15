package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

record EditGroupSnapshotRequest(
        @NotNull @Positive BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<PaymentRuleRequest> rules
) {
    FeeSelectionCampaignManagementPort.EditGroupSnapshotCommand toCommand() {
        String currency = yearlyFeeCurrency != null ? yearlyFeeCurrency : "CZK";
        Money yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(currency));
        List<MembershipPaymentRule> domainRules = rules == null ? List.of()
                : rules.stream().map(PaymentRuleRequest::toDomain).toList();
        return new FeeSelectionCampaignManagementPort.EditGroupSnapshotCommand(yearlyFee, domainRules);
    }
}
