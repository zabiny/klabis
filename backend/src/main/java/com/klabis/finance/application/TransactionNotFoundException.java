package com.klabis.finance.application;

import com.klabis.finance.domain.TransactionId;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID transactionId) {
        super("Transaction not found: " + transactionId);
    }

    public TransactionNotFoundException(TransactionId transactionId) {
        this(transactionId.value());
    }
}
