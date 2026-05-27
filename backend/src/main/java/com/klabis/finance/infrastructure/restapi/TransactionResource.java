package com.klabis.finance.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.finance.domain.Transaction;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Relation(collectionRelation = "transactions", itemRelation = "transaction")
@JsonInclude(JsonInclude.Include.NON_NULL)
record TransactionResource(
        UUID id,
        String type,
        BigDecimal amount,
        String currency,
        String note,
        Instant recordedAt,
        LocalDate occurredAt,
        UUID recordedBy,
        UUID reversesTransactionId
) {

    static TransactionResource from(Transaction tx) {
        return new TransactionResource(
                tx.getId().value(),
                tx.getType().name(),
                tx.getAmount().amount(),
                tx.getAmount().currency().getCurrencyCode(),
                tx.getNote(),
                tx.getRecordedAt(),
                tx.getOccurredAt(),
                tx.getRecordedBy().uuid(),
                tx.getReversesTransactionId() != null ? tx.getReversesTransactionId().value() : null
        );
    }
}
