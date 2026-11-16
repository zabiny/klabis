package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.LocalDate;

@PrimaryPort
public interface FeeYearLifecyclePort {

    void closeCampaign(FeeSelectionCampaignId campaignId);

    void processCampaignEnd(LocalDate toDate);
}
