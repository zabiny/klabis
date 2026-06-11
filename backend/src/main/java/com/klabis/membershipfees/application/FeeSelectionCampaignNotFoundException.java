package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.membershipfees.FeeSelectionCampaignId;

public class FeeSelectionCampaignNotFoundException extends BusinessRuleViolationException {

    public FeeSelectionCampaignNotFoundException(FeeSelectionCampaignId id) {
        super("FeeSelectionCampaign not found: " + id.value());
    }

    public FeeSelectionCampaignNotFoundException(int year) {
        super("No FeeSelectionCampaign found for year: " + year);
    }
}
