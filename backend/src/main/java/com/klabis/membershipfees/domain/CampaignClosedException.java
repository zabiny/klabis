package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

public class CampaignClosedException extends ConflictException implements UserFacingError {

    public CampaignClosedException() {
        super("Cannot modify a closed campaign — the voting deadline has already passed");
    }
}
