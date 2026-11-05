package com.klabis.groups.traininggroup.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class InvalidAgeRangeException extends InvalidDataException implements UserFacingError {

    private final int minAge;
    private final int maxAge;

    public InvalidAgeRangeException(int minAge, int maxAge) {
        super("maxAge must be >= minAge");
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("minAge", minAge, "maxAge", maxAge);
    }
}
