package com.klabis.finance.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@ValueObject
public final class Money {

    static final Currency CZK = Currency.getInstance("CZK");

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        Assert.notNull(amount, "Amount must not be null");
        Assert.notNull(currency, "Currency must not be null");
        this.amount = amount.stripTrailingZeros();
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money ofCzk(BigDecimal amount) {
        return new Money(amount, CZK);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, CZK);
    }

    public Money add(Money other) {
        Assert.isTrue(currency.equals(other.currency),
                () -> "Cannot add amounts in different currencies: " + currency + " and " + other.currency);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        Assert.isTrue(currency.equals(other.currency),
                () -> "Cannot subtract amounts in different currencies: " + currency + " and " + other.currency);
        return new Money(amount.subtract(other.amount), currency);
    }

    public int compareTo(Money other) {
        Assert.isTrue(currency.equals(other.currency),
                () -> "Cannot compare amounts in different currencies: " + currency + " and " + other.currency);
        return amount.compareTo(other.amount);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money other)) return false;
        return amount.compareTo(other.amount) == 0 && currency.equals(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
