package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class FeeSelectionCampaignNotFoundException extends ResourceNotFoundException implements UserFacingError {

    private final @Nullable Integer year;

    public FeeSelectionCampaignNotFoundException(FeeSelectionCampaignId id) {
        super("FeeSelectionCampaign not found: " + id.value());
        this.year = null;
    }

    public FeeSelectionCampaignNotFoundException(int year) {
        super("No FeeSelectionCampaign found for year: " + year);
        this.year = year;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return year == null ? Map.of() : Map.of("year", year);
    }
}
