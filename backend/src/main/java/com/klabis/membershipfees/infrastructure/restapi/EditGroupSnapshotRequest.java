package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.application.FeeYearPublicationManagementPort;
import com.klabis.membershipfees.domain.MembershipPaymentRuleSnapshot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

record EditGroupSnapshotRequest(
        @NotNull @Positive BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        List<CreateMembershipFeeLevelRequest.PaymentRuleRequest> rules
) {
    FeeYearPublicationManagementPort.EditGroupSnapshotCommand toCommand() {
        String currency = yearlyFeeCurrency != null ? yearlyFeeCurrency : "CZK";
        Money yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(currency));
        List<MembershipPaymentRuleSnapshot> domainRules = rules == null ? List.of()
                : rules.stream().map(r -> MembershipPaymentRuleSnapshot.from(r.toDomain())).toList();
        return new FeeYearPublicationManagementPort.EditGroupSnapshotCommand(yearlyFee, domainRules);
    }
}
