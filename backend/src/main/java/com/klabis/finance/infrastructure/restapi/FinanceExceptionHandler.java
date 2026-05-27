package com.klabis.finance.infrastructure.restapi;

import com.klabis.finance.domain.OverdraftLimitExceededException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
class FinanceExceptionHandler {

    @ExceptionHandler(OverdraftLimitExceededException.class)
    ErrorResponse handleOverdraftLimitExceeded(OverdraftLimitExceededException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Overdraft Limit Exceeded")
                .type(URI.create("OVERDRAFT_LIMIT_EXCEEDED"))
                .build();
    }
}
