package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
class ManualCampaignClosePortImpl implements ManualCampaignClosePort {

    private final FeeSelectionCampaignRepository campaignRepository;
    private final AllMembersPort allMembersPort;
    private final CampaignProcessor campaignProcessor;

    ManualCampaignClosePortImpl(FeeSelectionCampaignRepository campaignRepository,
                                AllMembersPort allMembersPort,
                                CampaignProcessor campaignProcessor) {
        this.campaignRepository = campaignRepository;
        this.allMembersPort = allMembersPort;
        this.campaignProcessor = campaignProcessor;
    }

    @Transactional
    @Override
    public void closeCampaign(FeeSelectionCampaignId campaignId) {
        FeeSelectionCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(campaignId));

        if (campaign.getDeadlineProcessedAt() != null) {
            throw new CampaignAlreadyProcessedException();
        }

        Set<MemberId> allMembers = allMembersPort.findAll();
        campaignProcessor.processPublication(campaign, allMembers);
    }
}
