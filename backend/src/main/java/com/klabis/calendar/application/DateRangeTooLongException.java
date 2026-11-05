package com.klabis.calendar.application;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class DateRangeTooLongException extends InvalidDataException implements UserFacingError {

    private final int maxDays;

    public DateRangeTooLongException(int maxDays, long requestedDays) {
        super("Date range must not exceed %d days. Requested range: %d days".formatted(maxDays, requestedDays));
        this.maxDays = maxDays;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("maxDays", maxDays);
    }
}
