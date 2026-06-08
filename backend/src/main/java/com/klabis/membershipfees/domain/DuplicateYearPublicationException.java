package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class DuplicateYearPublicationException extends BusinessRuleViolationException {

    public DuplicateYearPublicationException(int year) {
        super("Fee levels for year " + year + " have already been published");
    }
}
