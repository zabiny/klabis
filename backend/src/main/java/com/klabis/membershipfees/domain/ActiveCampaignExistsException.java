package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

public class ActiveCampaignExistsException extends ConflictException implements UserFacingError {

    public ActiveCampaignExistsException() {
        super("Cannot start a new campaign while another campaign is still active");
    }
}
