package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ConflictException;

public class CampaignClosedException extends ConflictException {

    public CampaignClosedException() {
        super("Cannot modify a closed campaign — the voting deadline has already passed");
    }
}
