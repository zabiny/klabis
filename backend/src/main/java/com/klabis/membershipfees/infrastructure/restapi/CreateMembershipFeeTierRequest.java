package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Currency;

record CreateMembershipFeeTierRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency
) {
    MembershipFeeTierManagementPort.CreateTierCommand toCommand() {
        String currency = yearlyFeeCurrency != null ? yearlyFeeCurrency : "CZK";
        Money yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(currency));
        return new MembershipFeeTierManagementPort.CreateTierCommand(name, yearlyFee);
    }
}
