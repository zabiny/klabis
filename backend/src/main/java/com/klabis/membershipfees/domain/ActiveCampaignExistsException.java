package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class ActiveCampaignExistsException extends BusinessRuleViolationException {

    public ActiveCampaignExistsException(int year) {
        super("Fee levels for year " + year + " have already been published");
    }
}
