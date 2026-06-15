package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class ActiveCampaignExistsException extends BusinessRuleViolationException {

    public ActiveCampaignExistsException() {
        super("Cannot start a new campaign while another campaign is still active");
    }
}
