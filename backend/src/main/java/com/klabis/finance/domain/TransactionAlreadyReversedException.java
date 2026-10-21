package com.klabis.finance.domain;

import com.klabis.common.exceptions.ConflictException;

public class TransactionAlreadyReversedException extends ConflictException {

    private final TransactionId transactionId;

    public TransactionAlreadyReversedException(TransactionId transactionId) {
        super("Transaction " + transactionId.value() + " has already been reversed");
        this.transactionId = transactionId;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }
}
