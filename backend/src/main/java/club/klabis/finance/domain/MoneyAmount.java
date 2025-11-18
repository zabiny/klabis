package club.klabis.finance.domain;

import org.springframework.util.Assert;

import java.math.BigDecimal;

public record MoneyAmount(BigDecimal amount) {
    public MoneyAmount {
        Assert.isTrue(amount.signum() != -1, "Money amount must not be negative");
    }

    public static MoneyAmount of(long amount) {
        return new MoneyAmount(BigDecimal.valueOf(amount));
    }

    public static MoneyAmount of(BigDecimal amount) {
        return new MoneyAmount(amount);
    }

    public static MoneyAmount ZERO = new MoneyAmount(BigDecimal.ZERO);

    public MoneyAmount add(MoneyAmount amount) {
        return new MoneyAmount(this.amount.add(amount.amount()));
    }

    public MoneyAmount subtract(MoneyAmount amount) {
        if (this.amount.compareTo(amount.amount()) < 0) {
            throw new IllegalStateException("Insufficient money to withdraw");
        }
        return new MoneyAmount(this.amount.subtract(amount.amount()));
    }

    public boolean isLowerThan(MoneyAmount amount) {
        return this.amount.compareTo(amount.amount()) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
