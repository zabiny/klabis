package com.klabis.finance.application;

import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;

import java.util.Optional;

public record TransactionWithReversal(Transaction transaction, Optional<TransactionId> reversedBy) {

    public static TransactionWithReversal withoutReversal(Transaction transaction) {
        return new TransactionWithReversal(transaction, Optional.empty());
    }

    public static TransactionWithReversal withReversal(Transaction transaction, TransactionId reversedBy) {
        return new TransactionWithReversal(transaction, Optional.of(reversedBy));
    }
}
