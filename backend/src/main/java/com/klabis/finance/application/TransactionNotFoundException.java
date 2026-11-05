package com.klabis.finance.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.finance.domain.TransactionId;
import com.klabis.members.MemberId;

public class TransactionNotFoundException extends ResourceNotFoundException implements UserFacingError {

    private final MemberId memberId;

    public TransactionNotFoundException(MemberId memberId, TransactionId transactionId) {
        super("Transaction not found: " + transactionId.value());
        this.memberId = memberId;
    }

    public MemberId memberId() {
        return memberId;
    }
}
