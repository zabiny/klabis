package com.klabis.calendar.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

public class CalendarItemEndBeforeStartException extends InvalidDataException implements UserFacingError {

    public CalendarItemEndBeforeStartException() {
        super("End date must be on or after start date");
    }

    @Override
    public String field() {
        return "endDate";
    }
}
