package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ConflictException;

public class ActiveCampaignExistsException extends ConflictException {

    public ActiveCampaignExistsException() {
        super("Cannot start a new campaign while another campaign is still active");
    }
}
