package club.klabis.finance.application;

import club.klabis.finance.domain.Account;
import club.klabis.finance.domain.MoneyAmount;

import java.math.BigDecimal;

public record DepositAction(BigDecimal amount) {

    public Account apply(Account targetAccount) {
        targetAccount.deposit(MoneyAmount.of(amount));
        return targetAccount;
    }

}
