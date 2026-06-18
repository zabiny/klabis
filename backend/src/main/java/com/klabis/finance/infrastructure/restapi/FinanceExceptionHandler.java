package com.klabis.finance.infrastructure.restapi;

import com.klabis.finance.domain.OverdraftLimitExceededException;
import com.klabis.finance.domain.TransactionAlreadyReversedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
class FinanceExceptionHandler {

    // Prepares HTTP 422 mapping for the registration-charge REST path (chargeForRegistration),
    // which currently has no caller — pending the event-registration change that will introduce it.
    @ExceptionHandler(OverdraftLimitExceededException.class)
    ErrorResponse handleOverdraftLimitExceeded(OverdraftLimitExceededException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Overdraft Limit Exceeded")
                .type(URI.create("OVERDRAFT_LIMIT_EXCEEDED"))
                .build();
    }

    @ExceptionHandler(TransactionAlreadyReversedException.class)
    ErrorResponse handleTransactionAlreadyReversed(TransactionAlreadyReversedException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage())
                .title("Transaction Already Reversed")
                .type(URI.create("TRANSACTION_ALREADY_REVERSED"))
                .build();
    }
}
