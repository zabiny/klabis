package com.klabis.finance.domain;

import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.time.LocalDate;

@Entity
public class Transaction {

    @Identity
    private final TransactionId id;
    private final TransactionType type;
    private final Money amount;
    private final String note;
    private final Instant recordedAt;
    private final LocalDate occurredAt;
    private final UserId recordedBy;
    private final TransactionId reversesTransactionId;

    private Transaction(TransactionId id, TransactionType type, Money amount, String note,
                        Instant recordedAt, LocalDate occurredAt, UserId recordedBy,
                        TransactionId reversesTransactionId) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.recordedAt = recordedAt;
        this.occurredAt = occurredAt;
        this.recordedBy = recordedBy;
        this.reversesTransactionId = reversesTransactionId;
    }

    static Transaction deposit(Money amount, String note, LocalDate occurredAt,
                               Instant recordedAt, UserId recordedBy) {
        return new Transaction(TransactionId.newId(), TransactionType.DEPOSIT, amount, note,
                recordedAt, occurredAt, recordedBy, null);
    }

    static Transaction charge(Money positiveChargeAmount, String note, LocalDate occurredAt,
                              Instant recordedAt, UserId recordedBy) {
        Money negativeAmount = Money.ofCzk(positiveChargeAmount.amount().negate());
        return new Transaction(TransactionId.newId(), TransactionType.OTHER, negativeAmount, note,
                recordedAt, occurredAt, recordedBy, null);
    }

    public static Transaction reconstruct(TransactionId id, TransactionType type, Money amount,
                                         String note, Instant recordedAt, LocalDate occurredAt,
                                         UserId recordedBy, TransactionId reversesTransactionId) {
        return new Transaction(id, type, amount, note, recordedAt, occurredAt,
                recordedBy, reversesTransactionId);
    }

    public TransactionId getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public LocalDate getOccurredAt() {
        return occurredAt;
    }

    public UserId getRecordedBy() {
        return recordedBy;
    }

    public TransactionId getReversesTransactionId() {
        return reversesTransactionId;
    }

    public boolean isReversal() {
        return reversesTransactionId != null;
    }
}
