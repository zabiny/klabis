package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
class CampaignEndProcessingPortImpl implements CampaignEndProcessingPort {

    private static final Logger log = LoggerFactory.getLogger(CampaignEndProcessingPortImpl.class);

    private final FeeSelectionCampaignRepository publicationRepository;
    private final AllMembersPort allMembersPort;
    private final CampaignProcessor campaignProcessor;

    CampaignEndProcessingPortImpl(FeeSelectionCampaignRepository publicationRepository,
                                   AllMembersPort allMembersPort,
                                   CampaignProcessor campaignProcessor) {
        this.publicationRepository = publicationRepository;
        this.allMembersPort = allMembersPort;
        this.campaignProcessor = campaignProcessor;
    }

    @Transactional
    @Override
    public void processCampaignEnd(LocalDate toDate) {

        List<FeeSelectionCampaign> unprocessed = publicationRepository.findUnprocessedClosedPublications(toDate);
        if (unprocessed.isEmpty()) {
            return;
        }
        log.info("Processing missed fee selections for {} closed publication(s)", unprocessed.size());

        Set<MemberId> allMembers = allMembersPort.findAll();

        for (FeeSelectionCampaign publication : unprocessed) {
            campaignProcessor.processPublication(publication, allMembers);
            publicationRepository.save(publication);
        }
    }

}
