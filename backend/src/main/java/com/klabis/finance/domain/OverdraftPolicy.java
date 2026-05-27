package com.klabis.finance.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record OverdraftPolicy(Money limit) {

    public OverdraftPolicy {
        Assert.notNull(limit, "Overdraft limit must not be null");
        Assert.isTrue(!limit.isPositive(), "Overdraft limit must be zero or negative");
    }

    public boolean allowsCharge(Money currentBalance, Money chargeAmount) {
        Assert.isTrue(chargeAmount.isPositive(), "Charge amount must be positive");
        Money resultingBalance = currentBalance.subtract(chargeAmount);
        return resultingBalance.compareTo(limit) >= 0;
    }
}
