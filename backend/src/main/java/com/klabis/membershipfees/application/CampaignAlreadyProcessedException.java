package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class CampaignAlreadyProcessedException extends BusinessRuleViolationException {

    public CampaignAlreadyProcessedException() {
        super("Campaign has already been processed — manual close is not allowed");
    }
}
