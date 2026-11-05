package com.klabis.finance.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

import java.util.Map;

public class OverdraftLimitExceededException extends BusinessRuleViolationException implements UserFacingError {

    private final MemberId memberId;
    private final Money currentBalance;
    private final Money chargeAmount;
    private final Money overdraftLimit;

    public OverdraftLimitExceededException(MemberId memberId, Money currentBalance, Money chargeAmount,
                                           Money overdraftLimit) {
        super("Charge of %s would push balance from %s below overdraft limit %s"
                .formatted(chargeAmount, currentBalance, overdraftLimit));
        this.memberId = memberId;
        this.currentBalance = currentBalance;
        this.chargeAmount = chargeAmount;
        this.overdraftLimit = overdraftLimit;
    }

    public MemberId memberId() {
        return memberId;
    }

    public Money getCurrentBalance() {
        return currentBalance;
    }

    public Money getChargeAmount() {
        return chargeAmount;
    }

    public Money getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of(
                "currentBalance", currentBalance.asMessageParameter(),
                "chargeAmount", chargeAmount.asMessageParameter(),
                "overdraftLimit", overdraftLimit.asMessageParameter());
    }
}
