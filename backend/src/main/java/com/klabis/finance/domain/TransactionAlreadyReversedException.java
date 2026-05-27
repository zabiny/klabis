package com.klabis.finance.domain;

public class TransactionAlreadyReversedException extends RuntimeException {

    private final TransactionId transactionId;

    public TransactionAlreadyReversedException(TransactionId transactionId) {
        super("Transaction " + transactionId.value() + " has already been reversed");
        this.transactionId = transactionId;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }
}
