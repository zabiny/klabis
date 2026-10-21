package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.ConflictException;

public class CampaignAlreadyProcessedException extends ConflictException {

    public CampaignAlreadyProcessedException() {
        super("Campaign has already been processed — manual close is not allowed");
    }
}
