package com.klabis.common.ratelimit;

import com.klabis.common.exceptions.UserFacingError;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitExceededException extends RuntimeException implements UserFacingError {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
