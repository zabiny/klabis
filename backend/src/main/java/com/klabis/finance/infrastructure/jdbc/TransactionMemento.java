package com.klabis.finance.infrastructure.jdbc;

import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

@Table("finance_transaction")
class TransactionMemento {

    @Id
    @Column("id")
    private UUID id;

    @Column("type")
    private String type;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("note")
    private String note;

    @Column("recorded_at")
    private Instant recordedAt;

    @Column("occurred_at")
    private LocalDate occurredAt;

    @Column("recorded_by_user_id")
    private UUID recordedByUserId;

    @Column("reverses_transaction_id")
    private UUID reversesTransactionId;

    protected TransactionMemento() {
    }

    static TransactionMemento from(Transaction tx) {
        TransactionMemento m = new TransactionMemento();
        m.id = tx.getId().value();
        m.type = tx.getType().name();
        m.amount = tx.getAmount().amount();
        m.currency = tx.getAmount().currency().getCurrencyCode();
        m.note = tx.getNote();
        m.recordedAt = tx.getRecordedAt();
        m.occurredAt = tx.getOccurredAt();
        m.recordedByUserId = tx.getRecordedBy().uuid();
        m.reversesTransactionId = tx.getReversesTransactionId() != null
                ? tx.getReversesTransactionId().value() : null;
        return m;
    }

    Transaction toTransaction() {
        TransactionId txId = new TransactionId(id);
        TransactionType txType = TransactionType.valueOf(type);
        Money txAmount = Money.of(amount, Currency.getInstance(currency));
        UserId recordedBy = new UserId(recordedByUserId);
        TransactionId reversesId = reversesTransactionId != null
                ? new TransactionId(reversesTransactionId) : null;
        return Transaction.reconstruct(txId, txType, txAmount, note, recordedAt, occurredAt, recordedBy, reversesId);
    }
}
