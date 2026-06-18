package com.klabis.finance.domain;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberAccount domain tests")
class MemberAccountTest {

    private final MemberId memberId = new MemberId(UUID.randomUUID());
    private final UserId financeManager = new UserId(UUID.randomUUID());
    private final Money depositAmount = Money.ofCzk(BigDecimal.valueOf(200));
    private final LocalDate today = LocalDate.now();
    private final Instant now = Instant.now();

    @Test
    @DisplayName("factory creates a new account with zero balance for a given MemberId")
    void createsNewAccountWithZeroBalance() {
        MemberAccount account = MemberAccount.openFor(memberId);

        assertThat(account.getId()).isEqualTo(memberId);
        assertThat(account.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("two accounts for different members are not equal")
    void accountsForDifferentMembersAreNotEqual() {
        MemberAccount account1 = MemberAccount.openFor(new MemberId(UUID.randomUUID()));
        MemberAccount account2 = MemberAccount.openFor(new MemberId(UUID.randomUUID()));

        assertThat(account1).isNotEqualTo(account2);
    }

    @Test
    @DisplayName("deposit with positive amount appends a DEPOSIT transaction and increases balance")
    void depositAppendsTransactionAndIncreasesBalance() {
        MemberAccount account = MemberAccount.openFor(memberId);

        Transaction tx = account.deposit(depositAmount, "test deposit", today, now, financeManager);

        assertThat(account.getBalance()).isEqualTo(depositAmount);
        assertThat(account.getTransactions()).hasSize(1);
        assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getAmount()).isEqualTo(depositAmount);
        assertThat(tx.getNote()).isEqualTo("test deposit");
        assertThat(tx.getOccurredAt()).isEqualTo(today);
        assertThat(tx.getRecordedBy()).isEqualTo(financeManager);
    }

    @Test
    @DisplayName("multiple deposits accumulate in the balance")
    void multipleDepositsAccumulateInBalance() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Money amount1 = Money.ofCzk(BigDecimal.valueOf(100));
        Money amount2 = Money.ofCzk(BigDecimal.valueOf(300));

        account.deposit(amount1, "first", today, now, financeManager);
        account.deposit(amount2, "second", today, now, financeManager);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(400)));
        assertThat(account.getTransactions()).hasSize(2);
    }

    @Test
    @DisplayName("deposit with zero amount throws domain exception")
    void depositWithZeroAmountThrows() {
        MemberAccount account = MemberAccount.openFor(memberId);

        assertThatThrownBy(() -> account.deposit(Money.zero(), "zero", today, now, financeManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deposit with negative amount throws domain exception")
    void depositWithNegativeAmountThrows() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Money negative = Money.ofCzk(BigDecimal.valueOf(-50));

        assertThatThrownBy(() -> account.deposit(negative, "negative", today, now, financeManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("chargeForRegistration with positive amount decreases balance and stores OTHER (negative) transaction when within overdraft limit")
    void chargeForRegistrationDecreasesBalanceAndStoresNegativeTransaction() {
        MemberAccount account = MemberAccount.openFor(memberId);
        account.deposit(Money.ofCzk(BigDecimal.valueOf(300)), "initial deposit", today, now, financeManager);
        Money chargeAmount = Money.ofCzk(BigDecimal.valueOf(100));
        OverdraftPolicy policy = new OverdraftPolicy(Money.ofCzk(BigDecimal.valueOf(-500)));

        Transaction tx = account.chargeForRegistration(chargeAmount, "test charge", today, now, financeManager, policy);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(200)));
        assertThat(account.getTransactions()).hasSize(2);
        assertThat(tx.getType()).isEqualTo(TransactionType.OTHER);
        assertThat(tx.getAmount()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(-100)));
        assertThat(tx.getNote()).isEqualTo("test charge");
        assertThat(tx.getOccurredAt()).isEqualTo(today);
        assertThat(tx.getRecordedBy()).isEqualTo(financeManager);
    }

    @Test
    @DisplayName("chargeForRegistration is allowed when resulting balance equals overdraft limit exactly")
    void chargeForRegistrationIsAllowedWhenBalanceEqualsLimit() {
        MemberAccount account = MemberAccount.openFor(memberId);
        OverdraftPolicy policy = new OverdraftPolicy(Money.ofCzk(BigDecimal.valueOf(-500)));
        // balance is 0, charge 500 → result is -500, which equals limit

        Transaction tx = account.chargeForRegistration(Money.ofCzk(BigDecimal.valueOf(500)), "edge charge", today, now, financeManager, policy);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(-500)));
        assertThat(tx.getType()).isEqualTo(TransactionType.OTHER);
    }

    @Test
    @DisplayName("chargeForRegistration is rejected when resulting balance would fall below overdraft limit")
    void chargeForRegistrationRejectedWhenBalanceWouldFallBelowOverdraftLimit() {
        MemberAccount account = MemberAccount.openFor(memberId);
        account.deposit(Money.ofCzk(BigDecimal.valueOf(100)), "initial", today, now, financeManager);
        // balance is 100, charge 700 → result -600, limit -500
        OverdraftPolicy policy = new OverdraftPolicy(Money.ofCzk(BigDecimal.valueOf(-500)));
        Money chargeAmount = Money.ofCzk(BigDecimal.valueOf(700));

        assertThatThrownBy(() -> account.chargeForRegistration(chargeAmount, "too large", today, now, financeManager, policy))
                .isInstanceOf(OverdraftLimitExceededException.class);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(100)));
        assertThat(account.getTransactions()).hasSize(1);
    }

    @Test
    @DisplayName("charge with zero amount throws domain exception")
    void chargeWithZeroAmountThrows() {
        MemberAccount account = MemberAccount.openFor(memberId);

        assertThatThrownBy(() -> account.charge(Money.zero(), "zero", today, now, financeManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("charge with negative amount throws domain exception")
    void chargeWithNegativeAmountThrows() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Money negative = Money.ofCzk(BigDecimal.valueOf(-50));

        assertThatThrownBy(() -> account.charge(negative, "negative", today, now, financeManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("charge records transaction and reduces balance even when result falls below overdraft limit")
    void chargeRecordsTransactionBelowOverdraftLimit() {
        MemberAccount account = MemberAccount.openFor(memberId);
        account.deposit(Money.ofCzk(BigDecimal.valueOf(100)), "initial", today, now, financeManager);
        // balance = 100; charge 500 → balance = -400, which is below a -500 overdraft limit
        // finance-manager charge is unrestricted — should succeed

        Transaction tx = account.charge(Money.ofCzk(BigDecimal.valueOf(500)), "manager charge", today, now, financeManager);

        assertThat(account.getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(-400)));
        assertThat(account.getTransactions()).hasSize(2);
        assertThat(tx.getType()).isEqualTo(TransactionType.OTHER);
        assertThat(tx.getAmount()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(-500)));
    }

    @Test
    @DisplayName("reverse of a deposit appends an OTHER transaction with negated amount referencing the original")
    void reverseOfDepositAppendsOppositeSignTransaction() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Transaction deposit = account.deposit(depositAmount, "initial", today, now, financeManager);

        Transaction reversal = account.reverse(deposit.getId(), "storno", today, now, financeManager);

        assertThat(reversal.getType()).isEqualTo(TransactionType.OTHER);
        assertThat(reversal.getAmount()).isEqualTo(depositAmount.negate());
        assertThat(reversal.getReversesTransactionId()).isEqualTo(deposit.getId());
        assertThat(reversal.isReversal()).isTrue();
        assertThat(account.getBalance()).isEqualTo(Money.zero());
        assertThat(account.getTransactions()).hasSize(2);
    }

    @Test
    @DisplayName("reverse of a charge appends a DEPOSIT transaction with positive amount referencing the original")
    void reverseOfChargeAppendsDepositTransaction() {
        MemberAccount account = MemberAccount.openFor(memberId);
        account.deposit(depositAmount, "initial", today, now, financeManager);
        Money chargeAmount = Money.ofCzk(BigDecimal.valueOf(50));
        Transaction charge = account.charge(chargeAmount, "charge", today, now, financeManager);

        Transaction reversal = account.reverse(charge.getId(), "storno", today, now, financeManager);

        assertThat(reversal.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(reversal.getAmount()).isEqualTo(chargeAmount);
        assertThat(reversal.getReversesTransactionId()).isEqualTo(charge.getId());
        assertThat(reversal.isReversal()).isTrue();
        assertThat(account.getBalance()).isEqualTo(depositAmount);
        assertThat(account.getTransactions()).hasSize(3);
    }

    @Test
    @DisplayName("reverse of an already-reversed transaction throws TransactionAlreadyReversedException")
    void reverseOfAlreadyReversedTransactionThrows() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Transaction deposit = account.deposit(depositAmount, "initial", today, now, financeManager);
        account.reverse(deposit.getId(), "first storno", today, now, financeManager);

        assertThatThrownBy(() -> account.reverse(deposit.getId(), "second storno", today, now, financeManager))
                .isInstanceOf(TransactionAlreadyReversedException.class);
    }

    @Test
    @DisplayName("reverse of a reversal (storno storna) is permitted as long as the reversal itself is not yet reversed")
    void reverseOfReversalIsPermitted() {
        MemberAccount account = MemberAccount.openFor(memberId);
        Transaction deposit = account.deposit(depositAmount, "initial", today, now, financeManager);
        Transaction reversal = account.reverse(deposit.getId(), "storno", today, now, financeManager);

        Transaction reversalOfReversal = account.reverse(reversal.getId(), "storno storna", today, now, financeManager);

        assertThat(reversalOfReversal.getReversesTransactionId()).isEqualTo(reversal.getId());
        assertThat(account.getTransactions()).hasSize(3);
    }

    @Test
    @DisplayName("reverse bypasses overdraft limit and can push balance below the limit")
    void reverseBypassesOverdraftLimit() {
        MemberAccount account2 = MemberAccount.openFor(new MemberId(UUID.randomUUID()));
        // balance = -500 (exactly at limit). Now reverse a prior deposit of 200 → balance = -700 (below limit)
        account2.deposit(Money.ofCzk(BigDecimal.valueOf(200)), "initial deposit", today, now, financeManager);
        account2.charge(Money.ofCzk(BigDecimal.valueOf(700)), "big charge", today, now, financeManager);
        Transaction depositTx = account2.getTransactions().get(0);
        // Reversing the deposit would push balance from -500 to -700 — reverse must succeed because storno bypasses overdraft check.
        Transaction bypassReversal = account2.reverse(depositTx.getId(), "bypass storno", today, now, financeManager);

        assertThat(bypassReversal).isNotNull();
        assertThat(account2.getBalance()).isEqualTo(Money.ofCzk(BigDecimal.valueOf(-700)));
    }
}
