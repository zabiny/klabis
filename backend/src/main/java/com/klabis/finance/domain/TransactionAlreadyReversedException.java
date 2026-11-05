package com.klabis.finance.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

import java.util.Map;

public class TransactionAlreadyReversedException extends ConflictException implements UserFacingError {

    private final MemberId memberId;
    private final TransactionId transactionId;
    private final Money amount;

    public TransactionAlreadyReversedException(MemberId memberId, TransactionId transactionId, Money amount) {
        super("Transaction " + transactionId.value() + " has already been reversed");
        this.memberId = memberId;
        this.transactionId = transactionId;
        this.amount = amount;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public MemberId memberId() {
        return memberId;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("amount", amount.asMessageParameter());
    }
}
