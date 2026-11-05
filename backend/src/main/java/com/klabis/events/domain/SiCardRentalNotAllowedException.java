package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class SiCardRentalNotAllowedException extends BusinessRuleViolationException implements UserFacingError {

    public SiCardRentalNotAllowedException() {
        super("SI card rental is not allowed for this event");
    }

    @Override
    public String field() {
        return "wantsToRentSi";
    }
}
