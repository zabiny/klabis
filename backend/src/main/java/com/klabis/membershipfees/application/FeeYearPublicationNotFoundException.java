package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.membershipfees.FeeYearPublicationId;

public class FeeYearPublicationNotFoundException extends BusinessRuleViolationException {

    public FeeYearPublicationNotFoundException(FeeYearPublicationId id) {
        super("FeeYearPublication not found: " + id.value());
    }
}
