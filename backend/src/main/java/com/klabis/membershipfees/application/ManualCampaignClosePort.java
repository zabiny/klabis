package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface ManualCampaignClosePort {

    void closeCampaign(FeeSelectionCampaignId campaignId);

}
