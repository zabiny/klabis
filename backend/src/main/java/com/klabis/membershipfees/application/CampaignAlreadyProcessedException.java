package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

public class CampaignAlreadyProcessedException extends ConflictException implements UserFacingError {

    public CampaignAlreadyProcessedException() {
        super("Campaign has already been processed — manual close is not allowed");
    }
}
