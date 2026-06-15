package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class CampaignClosedException extends BusinessRuleViolationException {

    public CampaignClosedException() {
        super("Cannot modify a closed campaign — the voting deadline has already passed");
    }
}
