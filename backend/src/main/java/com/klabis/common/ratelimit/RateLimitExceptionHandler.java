package com.klabis.common.ratelimit;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RateLimitExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RateLimitExceptionHandler.class);

    @ExceptionHandler({RateLimitExceededException.class, RequestNotPermitted.class})
    public ErrorResponse handleRateLimitExceeded(Exception ex) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());

        return ErrorResponse.builder(ex, HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later")
                .title("Too Many Requests")
                .header("Retry-After", "3600")
                .build();
    }
}
