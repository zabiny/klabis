package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.FeeSelectionCampaign;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record FeeSelectionCampaignResponse(
        UUID id,
        int year,
        LocalDate votingDeadline,
        Instant deadlineProcessedAt
) {
    static FeeSelectionCampaignResponse from(FeeSelectionCampaign publication) {
        return new FeeSelectionCampaignResponse(
                publication.getId().value(),
                publication.getYear(),
                publication.getVotingDeadline(),
                publication.getDeadlineProcessedAt()
        );
    }
}
