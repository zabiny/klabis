package com.klabis.finance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money value object tests")
class MoneyTest {

    @Test
    @DisplayName("zero() returns CZK zero amount")
    void zeroReturnsCzkZero() {
        Money zero = Money.zero();

        assertThat(zero.isZero()).isTrue();
        assertThat(zero.currency()).isEqualTo(Currency.getInstance("CZK"));
    }

    @Nested
    @DisplayName("add()")
    class Add {

        @Test
        @DisplayName("adds two positive amounts")
        void addsTwoPositiveAmounts() {
            Money a = Money.ofCzk(new BigDecimal("100"));
            Money b = Money.ofCzk(new BigDecimal("50"));

            Money result = a.add(b);

            assertThat(result).isEqualTo(Money.ofCzk(new BigDecimal("150")));
        }

        @Test
        @DisplayName("throws when currencies differ")
        void throwsWhenCurrenciesDiffer() {
            Money czk = Money.ofCzk(new BigDecimal("100"));
            Money eur = Money.of(new BigDecimal("100"), Currency.getInstance("EUR"));

            assertThatThrownBy(() -> czk.add(eur))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different currencies");
        }
    }

    @Nested
    @DisplayName("subtract()")
    class Subtract {

        @Test
        @DisplayName("subtracts amount resulting in negative balance")
        void subtractsToNegativeBalance() {
            Money a = Money.ofCzk(new BigDecimal("50"));
            Money b = Money.ofCzk(new BigDecimal("100"));

            Money result = a.subtract(b);

            assertThat(result.isNegative()).isTrue();
            assertThat(result).isEqualTo(Money.ofCzk(new BigDecimal("-50")));
        }

        @Test
        @DisplayName("throws when currencies differ")
        void throwsWhenCurrenciesDiffer() {
            Money czk = Money.ofCzk(new BigDecimal("100"));
            Money eur = Money.of(new BigDecimal("50"), Currency.getInstance("EUR"));

            assertThatThrownBy(() -> czk.subtract(eur))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different currencies");
        }
    }

    @Nested
    @DisplayName("sign checks")
    class SignChecks {

        @Test
        @DisplayName("isPositive() is true for positive amount")
        void isPositiveForPositiveAmount() {
            assertThat(Money.ofCzk(new BigDecimal("1")).isPositive()).isTrue();
        }

        @Test
        @DisplayName("isNegative() is true for negative amount")
        void isNegativeForNegativeAmount() {
            assertThat(Money.ofCzk(new BigDecimal("-1")).isNegative()).isTrue();
        }

        @Test
        @DisplayName("isZero() is true for zero amount")
        void isZeroForZeroAmount() {
            assertThat(Money.zero().isZero()).isTrue();
        }
    }

    @Test
    @DisplayName("equality ignores trailing zeros")
    void equalityIgnoresTrailingZeros() {
        Money a = Money.ofCzk(new BigDecimal("100.00"));
        Money b = Money.ofCzk(new BigDecimal("100"));

        assertThat(a).isEqualTo(b);
    }
}
