package com.klabis.finance.domain;

public class OverdraftLimitExceededException extends RuntimeException {

    private final Money currentBalance;
    private final Money chargeAmount;
    private final Money overdraftLimit;

    public OverdraftLimitExceededException(Money currentBalance, Money chargeAmount, Money overdraftLimit) {
        super("Charge of %s would push balance from %s below overdraft limit %s"
                .formatted(chargeAmount, currentBalance, overdraftLimit));
        this.currentBalance = currentBalance;
        this.chargeAmount = chargeAmount;
        this.overdraftLimit = overdraftLimit;
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
}
