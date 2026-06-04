package com.klabis.events.domain;

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

    public static Currency parseCurrency(String code) {
        if (code == null || code.isBlank()) {
            return CZK;
        }
        try {
            return Currency.getInstance(code.trim());
        } catch (IllegalArgumentException e) {
            return CZK;
        }
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
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
