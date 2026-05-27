package com.klabis.finance.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AggregateRoot
public class MemberAccount extends KlabisAggregateRoot<MemberAccount, MemberId> {

    @Identity
    private final MemberId id;

    private Money balance;

    private final List<Transaction> transactions;

    private MemberAccount(MemberId id, Money balance, List<Transaction> transactions) {
        this.id = id;
        this.balance = balance;
        this.transactions = new ArrayList<>(transactions);
    }

    public static MemberAccount openFor(MemberId memberId) {
        return new MemberAccount(memberId, Money.zero(), List.of());
    }

    public static MemberAccount reconstruct(MemberId id, Money balance, List<Transaction> transactions) {
        return new MemberAccount(id, balance, transactions);
    }

    public Transaction deposit(Money amount, String note, LocalDate occurredAt,
                               Instant recordedAt, UserId recordedBy) {
        Assert.isTrue(amount.isPositive(), "Deposit amount must be positive");
        Transaction tx = Transaction.deposit(amount, note, occurredAt, recordedAt, recordedBy);
        transactions.add(tx);
        balance = balance.add(amount);
        return tx;
    }

    public Transaction charge(Money amount, String note, LocalDate occurredAt,
                              Instant recordedAt, UserId recordedBy, OverdraftPolicy overdraftPolicy) {
        Assert.isTrue(amount.isPositive(), "Charge amount must be positive");
        if (!overdraftPolicy.allowsCharge(balance, amount)) {
            throw new OverdraftLimitExceededException(balance, amount, overdraftPolicy.limit());
        }
        Transaction tx = Transaction.charge(amount, note, occurredAt, recordedAt, recordedBy);
        transactions.add(tx);
        balance = balance.subtract(amount);
        return tx;
    }

    public Transaction reverse(TransactionId transactionId, String note, LocalDate occurredAt,
                               Instant recordedAt, UserId recordedBy) {
        Transaction original = transactions.stream()
                .filter(tx -> tx.getId().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction " + transactionId.value() + " not found on this account"));

        boolean alreadyReversed = transactions.stream()
                .anyMatch(tx -> transactionId.equals(tx.getReversesTransactionId()));
        if (alreadyReversed) {
            throw new TransactionAlreadyReversedException(transactionId);
        }

        Transaction reversal = Transaction.reversal(original, note, occurredAt, recordedAt, recordedBy);
        transactions.add(reversal);
        balance = balance.add(reversal.getAmount());
        return reversal;
    }

    @Override
    public MemberId getId() {
        return id;
    }

    public Money getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}
