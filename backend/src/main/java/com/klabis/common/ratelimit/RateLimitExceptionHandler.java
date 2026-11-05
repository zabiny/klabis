package com.klabis.common.ratelimit;

import com.klabis.common.exceptions.CommonErrorTypes;
import com.klabis.common.exceptions.UserFacingError;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class RateLimitExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RateLimitExceptionHandler.class);

    private static final String RATE_LIMIT_EXCEEDED = "rate-limit-exceeded";

    @ExceptionHandler({RateLimitExceededException.class, RequestNotPermitted.class})
    public ErrorResponse handleRateLimitExceeded(Exception ex) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());

        return ErrorResponse.builder(ex, HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later")
                .title("Too Many Requests")
                .type(URI.create(CommonErrorTypes.uri(
                        ex instanceof UserFacingError typed ? typed.errorType() : RATE_LIMIT_EXCEEDED)))
                .header("Retry-After", "3600")
                .build();
    }
}
